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

val defaultDevBaseUrl = "http://192.168.0.141:3005"
val localPropertiesFile = rootProject.layout.projectDirectory.file("local.properties")
val devBaseUrl: String = if (localPropertiesFile.asFile.exists()) {
    providers.fileContents(localPropertiesFile).asText
        .map { text ->
            val properties = Properties().apply { text.reader().use { load(it) } }
            properties.getProperty("devBaseUrl") ?: run {
                logger.warn(
                    "local.properties found but 'devBaseUrl' is not set; " +
                        "falling back to $defaultDevBaseUrl"
                )
                defaultDevBaseUrl
            }
        }
        .get()
} else {
    logger.warn(
        "local.properties not found; debug builds will use $defaultDevBaseUrl. " +
            "Set 'devBaseUrl' in local.properties to override."
    )
    defaultDevBaseUrl
}

require(devBaseUrl.none { it == '"' || it == '\\' || it == '<' || it == '>' || it == '&' || it.isWhitespace() }) {
    "devBaseUrl must not contain quotes, backslashes, XML metacharacters, or whitespace"
}

val releaseBaseUrl = "https://shoppinglist.pascariu.eu"
require(releaseBaseUrl.startsWith("https://")) {
    "release BASE_URL must use HTTPS"
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
        }
        release {
            optimization {
                enable = true
                // Gradual R8 adoption: start with known-safe libraries, expand later.
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            buildConfigField("String", "BASE_URL", "\"$releaseBaseUrl\"")
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
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

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.okhttp.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
