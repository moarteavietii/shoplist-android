pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "shoplist-android"
include(":app")

val localProperties = File(rootDir, "local.properties")
if (!localProperties.isFile) {
    val mainCheckoutLocalProperties = runCatching {
        val process = ProcessBuilder("git", "rev-parse", "--path-format=absolute", "--git-common-dir")
            .directory(rootDir)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }.trim()
        process.waitFor()
        if (process.exitValue() == 0 && output.isNotEmpty()) {
            File(File(output).canonicalFile.parentFile, "local.properties")
        } else {
            null
        }
    }.getOrNull()

    when {
        mainCheckoutLocalProperties?.isFile == true &&
            mainCheckoutLocalProperties.canonicalFile.parentFile != rootDir.canonicalFile -> {
            mainCheckoutLocalProperties.copyTo(localProperties)
            println("local.properties missing; copied from main checkout ${mainCheckoutLocalProperties.parentFile}")
        }
        System.getenv("ANDROID_HOME") != null -> {
            localProperties.writeText("sdk.dir=${System.getenv("ANDROID_HOME")}")
            println("local.properties missing; synthesized sdk.dir from ANDROID_HOME")
        }
        else -> println(
            "local.properties missing and ANDROID_HOME unset; " +
                "create it with sdk.dir/devBaseUrl if the build fails"
        )
    }
}
 