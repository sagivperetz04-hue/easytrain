plugins {
    alias(libs.plugins.easytrain.android.library)
    alias(libs.plugins.easytrain.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.easytrain.core.network"
}

dependencies {
    api(platform(libs.supabase.bom))
    api(libs.supabase.auth)
    api(libs.supabase.postgrest)
    api(libs.supabase.realtime)
    api(libs.supabase.storage)
    api(libs.supabase.functions)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.serialization.json)
}
