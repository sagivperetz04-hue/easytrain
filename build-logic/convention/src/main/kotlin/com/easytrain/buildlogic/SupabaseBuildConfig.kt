package com.easytrain.buildlogic

import com.android.build.api.dsl.CommonExtension
import java.util.Properties
import org.gradle.api.Project

/**
 * Exposes the Supabase URL and publishable key as `BuildConfig` fields. Values come from
 * `local.properties` first, then the environment (CI). A missing value fails the build instead of
 * producing an app that points nowhere. The publishable key is safe on the client; the service-role
 * key must never appear here.
 */
internal fun Project.configureSupabaseBuildConfig(extension: CommonExtension) {
    extension.buildFeatures.buildConfig = true
    extension.defaultConfig.buildConfigField("String", "SUPABASE_URL", "\"${requiredSecret("SUPABASE_URL")}\"")
    extension.defaultConfig.buildConfigField(
        "String",
        "SUPABASE_PUBLISHABLE_KEY",
        "\"${requiredSecret("SUPABASE_PUBLISHABLE_KEY")}\"",
    )
}

private fun Project.requiredSecret(key: String): String {
    val fromLocalProperties = localProperties[key]?.toString()?.takeIf(String::isNotBlank)
    val fromEnvironment = providers.environmentVariable(key).orNull?.takeIf(String::isNotBlank)
    return fromLocalProperties ?: fromEnvironment ?: error(
        "$key is missing. Copy local.properties.example to local.properties and fill it in " +
            "(values come from `supabase status`), or set $key in the environment.",
    )
}

private val Project.localProperties: Properties
    get() = Properties().apply {
        val file = rootProject.file("local.properties")
        if (file.exists()) {
            file.inputStream().use(::load)
        }
    }
