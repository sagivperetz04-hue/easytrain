import com.android.build.api.dsl.ApplicationExtension
import com.easytrain.buildlogic.configureAndroid
import com.easytrain.buildlogic.configureCompose
import com.easytrain.buildlogic.configureSupabaseBuildConfig
import com.easytrain.buildlogic.libs
import com.easytrain.buildlogic.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")

            extensions.configure<ApplicationExtension> {
                configureAndroid(this)
                configureCompose(this)
                configureSupabaseBuildConfig(this)
                defaultConfig.targetSdk = libs.version("targetSdk").toInt()

                buildTypes.getByName("release") {
                    isMinifyEnabled = true
                    isShrinkResources = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro",
                    )
                }
            }
        }
    }
}
