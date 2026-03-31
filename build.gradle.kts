plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.benManesVersions)
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
