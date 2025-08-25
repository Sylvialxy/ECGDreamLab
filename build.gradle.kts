// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        // 使用阿里云镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 备用镜像
        maven { url = uri("https://mirrors.tencent.com/nexus/repository/maven-public/") }
        // 原始仓库作为备用
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

// 移除 allprojects 块，因为仓库配置已经在 settings.gradle.kts 中定义

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
}