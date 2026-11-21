pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // TinyPinyin (Chinese readings) was only ever published to JCenter; this mirror still holds it, and nothing else is taken from it
        maven {
            setUrl("https://maven.aliyun.com/repository/public")
            content { includeGroup("com.github.promeG") }
        }
    }
}
rootProject.name = "milo-native"
include(":app")
