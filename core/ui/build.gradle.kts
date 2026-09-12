plugins {
    alias(libs.plugins.easytrain.android.library.compose)
}

android {
    namespace = "com.easytrain.core.ui"
}

dependencies {
    api(project(":core:designsystem"))
    implementation(project(":core:common"))
}
