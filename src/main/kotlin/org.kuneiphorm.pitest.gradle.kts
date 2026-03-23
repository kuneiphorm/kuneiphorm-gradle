plugins {
    id("info.solidsoft.pitest")
}

configure<info.solidsoft.gradle.pitest.PitestPluginExtension> {
    junit5PluginVersion.set("1.2.1")
    timestampedReports.set(false)
}
