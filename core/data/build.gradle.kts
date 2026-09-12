plugins {
    alias(libs.plugins.easytrain.android.library)
    alias(libs.plugins.easytrain.hilt)
}

android {
    namespace = "com.easytrain.core.data"
}

dependencies {
    api(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
}
