plugins {
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

repositories {
  mavenCentral()
  gradlePluginPortal() // to resolve external plugins dependencies
}

dependencies {
  implementation(plugin(libs.plugins.kotlinPlugin))
  implementation(plugin(libs.plugins.intelliJPlatformPlugin))
  implementation(plugin(libs.plugins.propertiesPlugin))
  implementation(plugin(libs.plugins.testRetryPlugin))
}

// Helper function that transforms a Gradle Plugin alias from a
// Version Catalog into a valid dependency notation for buildSrc
// Taken from https://docs.gradle.org/current/userguide/version_catalogs.html#sec:buildsrc-version-catalog
fun DependencyHandlerScope.plugin(plugin: Provider<PluginDependency>): Provider<String> =
  plugin.map { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" }
