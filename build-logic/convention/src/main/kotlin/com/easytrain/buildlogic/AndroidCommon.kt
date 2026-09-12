package com.easytrain.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal fun Project.configureAndroid(extension: CommonExtension) {
    with(extension) {
        compileSdk = libs.version("compileSdk").toInt()
        defaultConfig.minSdk = libs.version("minSdk").toInt()

        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17

        testOptions.unitTests.isIncludeAndroidResources = true
        testOptions.unitTests.isReturnDefaultValues = true
    }

    // Modules that have no tests yet still get generated test sources from KSP, which Gradle 9
    // treats as a misconfiguration. Real tests arrive with each feature.
    tasks.withType<Test>().configureEach {
        failOnNoDiscoveredTests.set(false)
    }

    dependencies {
        add("implementation", libs.library("kotlinx-coroutines-android"))
        add("testImplementation", libs.library("junit"))
        add("testImplementation", libs.library("kotlinx-coroutines-test"))
        add("testImplementation", libs.library("turbine"))
    }
}
