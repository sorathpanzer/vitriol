plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.benManesVersions)
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

// tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
//     notCompatibleWithConfigurationCache("DependencyUpdatesTask is not config-cache compatible")

//     rejectVersionIf {
//         listOf("alpha", "beta", "rc").any {
//             candidate.version.contains(it, ignoreCase = true)
//         }
//     }
// }



// tasks.withType<com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask> {
//     rejectVersionIf {
//         candidate.version.contains("alpha", false) ||
//         candidate.version.contains("beta", false) ||
//         candidate.version.contains("rc", false)
//     }
// }
