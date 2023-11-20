plugins {
  id("org.jetbrains.fleet-plugin") version "0.2.99"
}

fleetPlugin {
  id = "com.jetbrains.edu.fleet"

  metadata {
    readableName = "JetBrains Academy"
  }
  fleetRuntime {
    version = gradleProperty("fleetRuntimeVersion")
    minVersion = gradleProperty("fleetRuntimeMinVersion")
    maxVersion = gradleProperty("fleetRuntimeMaxVersion")
  }
}

fun gradleProperty(key: String): Provider<String> = providers.gradleProperty(key)
