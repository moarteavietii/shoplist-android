import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val defaultDevBaseUrl = "http://192.168.0.141:3005"
val localPropertiesFile = rootProject.layout.projectDirectory.file("local.properties")
val localProperties: Properties? = if (localPropertiesFile.asFile.exists()) {
    Properties().apply {
        providers.fileContents(localPropertiesFile).asText.get().reader().use { load(it) }
    }
} else {
    null
}

val devBaseUrl: String = if (localProperties == null) {
    logger.warn(
        "local.properties not found; debug builds will use $defaultDevBaseUrl. " +
            "Set 'devBaseUrl' in local.properties to override."
    )
    defaultDevBaseUrl
} else {
    localProperties.getProperty("devBaseUrl") ?: run {
        logger.warn(
            "local.properties found but 'devBaseUrl' is not set; " +
                "falling back to $defaultDevBaseUrl"
        )
        defaultDevBaseUrl
    }
}

require(devBaseUrl.none { it == '"' || it == '\\' || it == '<' || it == '>' || it == '&' || it.isWhitespace() }) {
    "devBaseUrl must not contain quotes, backslashes, XML metacharacters, or whitespace"
}

val mockCloudflare: Boolean = if (localProperties == null) {
    false
} else {
    localProperties.keys
        .map { it.toString() }
        .firstOrNull { it.equals("mockCloudflare", ignoreCase = true) && it != "mockCloudflare" }
        ?.let { key ->
            logger.warn(
                "local.properties has '$key' but the expected key is 'mockCloudflare' " +
                    "(keys are case-sensitive); the mock flag is OFF"
            )
        }
    when (val raw = localProperties.getProperty("mockCloudflare")?.trim()?.lowercase()) {
        null, "" -> false
        "true" -> true
        "false" -> false
        else -> {
            logger.warn(
                "local.properties 'mockCloudflare=$raw' is not a valid boolean " +
                    "(expected true or false); treating as false"
            )
            false
        }
    }
}

val releaseBaseUrl = "https://shoppinglist.pascariu.eu"
require(releaseBaseUrl.startsWith("https://")) {
    "release BASE_URL must use HTTPS"
}

val keystoreProperties: Properties? = run {
    val props = Properties()
    localProperties?.let { props.putAll(it) }
    val userGradleProps = File(System.getProperty("user.home"), ".gradle/gradle.properties")
    if (userGradleProps.exists()) {
        userGradleProps.reader().use { props.load(it) }
    }
    if (props.isEmpty) null else props
}

fun Properties.nonBlankProperty(key: String): String? =
    getProperty(key)?.takeIf { it.isNotBlank() }

val keystorePath: String? = keystoreProperties?.nonBlankProperty("shoplist.release.keystore.path")
val keystorePassword: String? = keystoreProperties?.nonBlankProperty("shoplist.release.keystore.password")
val keystoreAlias: String? = keystoreProperties?.nonBlankProperty("shoplist.release.keystore.alias")
val keystoreKeyPassword: String? =
    keystoreProperties?.nonBlankProperty("shoplist.release.keystore.keypassword")

val keystoreFile: File? = keystorePath?.let {
    val expanded = File(it.replaceFirst("^~", System.getProperty("user.home")))
    if (expanded.isAbsolute) expanded else rootDir.resolve(expanded)
}

val hasReleaseSigningConfig = keystoreFile != null && keystorePassword != null && keystoreAlias != null

fun requireReleaseSigningConfig() {
    if (hasReleaseSigningConfig) return
    val missing = buildList {
        if (keystorePath == null) add("shoplist.release.keystore.path")
        if (keystorePassword == null) add("shoplist.release.keystore.password")
        if (keystoreAlias == null) add("shoplist.release.keystore.alias")
    }
    throw GradleException(
        "Release signing configuration is missing. Set the following in " +
            "local.properties (or ~/.gradle/gradle.properties): ${missing.joinToString()}. " +
            "Never commit these values or the keystore file."
    )
}

gradle.taskGraph.whenReady {
    if (allTasks.any { it.project.path == ":app" && it.name.contains("Release") }) {
        requireReleaseSigningConfig()
    }
}

val devHost = devBaseUrl
    .removePrefix("http://")
    .removePrefix("https://")
    .substringBefore('/')
    .substringBefore(':')
require(devHost.isNotEmpty()) {
    "devBaseUrl must include a host (got '$devBaseUrl')"
}

val debugResDir = layout.buildDirectory.dir("generated/dev-network-security-config/res")

abstract class GenerateDevNetworkSecurityConfig : DefaultTask() {
    @get:Input
    abstract val devHost: Property<String>

    @get:OutputDirectory
    abstract val outputResDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val host = devHost.get()
        val target = outputResDir.get().file("xml/network_security_config.xml").asFile
        target.parentFile.mkdirs()
        target.writeText(
            """
            <?xml version="1.0" encoding="utf-8"?>
            <network-security-config>
                <domain-config cleartextTrafficPermitted="true">
                    <domain includeSubdomains="false">$host</domain>
                    <domain includeSubdomains="false">10.0.2.2</domain>
                </domain-config>
            </network-security-config>
            """.trimIndent()
        )
    }
}

val resolvedDevHost = devHost
val generateDevNetworkSecurityConfig = tasks.register<GenerateDevNetworkSecurityConfig>("generateDevNetworkSecurityConfig") {
    devHost.set(resolvedDevHost)
    outputResDir.set(debugResDir)
}

android {
    namespace = "eu.pascariu.shoppinglist"
    compileSdk {
        version = release(37)
    }

    signingConfigs {
        if (hasReleaseSigningConfig) {
            val keystore = keystoreFile!!
            require(keystore.isFile) {
                "Release signing: keystore file not found at '$keystore' " +
                    "(shoplist.release.keystore.path)"
            }
            create("release") {
                storeFile = keystore
                storePassword = keystorePassword
                keyAlias = keystoreAlias
                keyPassword = keystoreKeyPassword ?: keystorePassword
            }
        }
    }

    defaultConfig {
        applicationId = "eu.pascariu.shoppinglist"
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "eu.pascariu.shoppinglist.HiltTestRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$devBaseUrl\"")
            buildConfigField("boolean", "MOCK_CLOUDFLARE", mockCloudflare.toString())
            buildConfigField("boolean", "REQUIRES_CREDENTIALS", mockCloudflare.toString())
        }
        release {
            optimization {
                enable = true
                // Gradual R8 adoption: start with known-safe libraries, expand later.
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            buildConfigField("String", "BASE_URL", "\"$releaseBaseUrl\"")
            buildConfigField("boolean", "REQUIRES_CREDENTIALS", "true")
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
        unitTests.isIncludeAndroidResources = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

androidComponents {
    onVariants { variant ->
        if (variant.buildType == "debug") {
            variant.sources.res?.addGeneratedSourceDirectory(
                generateDevNetworkSecurityConfig,
                GenerateDevNetworkSecurityConfig::outputResDir
            )
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.process)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.errorprone.annotations)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.okhttp.sse)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.reorderable)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.room.testing)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
