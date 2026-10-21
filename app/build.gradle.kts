import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val defaultDevBaseUrl = "http://10.0.2.2:3000"
val localPropertiesFile = rootProject.layout.projectDirectory.file("local.properties")
val devBaseUrl: String = if (localPropertiesFile.asFile.exists()) {
    providers.fileContents(localPropertiesFile).asText
        .map { text ->
            val properties = Properties().apply { text.reader().use { load(it) } }
            properties.getProperty("shoplist.devBaseUrl") ?: run {
                logger.warn(
                    "local.properties found but 'shoplist.devBaseUrl' is not set; " +
                        "falling back to $defaultDevBaseUrl"
                )
                defaultDevBaseUrl
            }
        }
        .get()
} else {
    logger.warn(
        "local.properties not found; debug builds will use $defaultDevBaseUrl. " +
            "Set 'shoplist.devBaseUrl' in local.properties to override."
    )
    defaultDevBaseUrl
}

require(devBaseUrl.none { it == '"' || it == '\\' || it.isWhitespace() }) {
    "shoplist.devBaseUrl must not contain quotes, backslashes, or whitespace"
}

val releaseBaseUrl = "https://shoppinglist.pascariu.eu"
require(releaseBaseUrl.startsWith("https://")) {
    "release BASE_URL must use HTTPS"
}

android {
    namespace = "com.example.shoplist_android"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.shoplist_android"
        minSdk = 35
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"$devBaseUrl\"")
        }
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
            buildConfigField("String", "BASE_URL", "\"$releaseBaseUrl\"")
        }
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.security.crypto)

    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.accompanist.permissions)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
