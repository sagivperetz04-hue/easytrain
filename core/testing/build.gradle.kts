plugins {
    alias(libs.plugins.easytrain.android.library)
}

android {
    namespace = "com.easytrain.core.testing"
}

dependencies {
    api(project(":core:data"))
    api(project(":core:common"))
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
}
