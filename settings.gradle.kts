pluginManagement {
    repositories {
        // 使用阿里云镜像优先
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // 腾讯云镜像作为备用
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
        // 原始仓库作为最后备用
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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 使用阿里云镜像优先
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 腾讯云镜像作为备用
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
        // JitPack 仓库
        maven { url = uri("https://jitpack.io") }
        // 原始仓库作为最后备用
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "NeuroSleep"
include(":app")
 