import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.flyfishxu.usage.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Usage Receipt"
            packageVersion = "1.0.0"

            macOS {
                bundleID = "com.flyfishxu.usage"
                iconFile.set(project.file("../assets/app-icon/usage-receipt-macos.icns"))
            }
            windows {
                iconFile.set(project.file("../assets/app-icon/usage-receipt.ico"))
            }
            linux {
                packageName = "usage-receipt"
                iconFile.set(project.file("../assets/app-icon/usage-receipt.png"))
            }
        }
    }
}
