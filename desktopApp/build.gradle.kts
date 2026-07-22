import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        load(f.inputStream())
    }
}

tasks.withType<JavaExec>().configureEach {
    if (name == "run") {
        environment("OPENAI_API_KEY", localProps.getProperty("OPENAI_API_KEY", ""))
        environment("CENSUS_API_KEY", localProps.getProperty("CENSUS_API_KEY", ""))
    }
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.app.backendplug_kmp.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.app.backendplug_kmp"
            packageVersion = "1.0.0"
        }
    }
}