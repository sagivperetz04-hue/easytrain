plugins {
    alias(libs.plugins.easytrain.android.library.compose)
}

android {
    namespace = "com.easytrain.core.ui"
}

dependencies {
    implementation(project(":core:designsystem"))
}
