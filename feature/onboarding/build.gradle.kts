plugins {
    alias(libs.plugins.easytrain.android.feature)
}

android {
    namespace = "com.easytrain.feature.onboarding"
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:data"))
}
