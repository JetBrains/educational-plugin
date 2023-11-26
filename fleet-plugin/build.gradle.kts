import org.jetbrains.fleet.dsl.FleetKotlinDependencyHandler

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

  layers {
    commonImpl {
      dependencies {
        api(project(":edu-format"))
        apiWithoutKotlin(rootProject.libs.jackson.module.kotlin)
        apiWithoutKotlin(rootProject.libs.jackson.dataformat.yaml)
        apiWithoutKotlin(rootProject.libs.jackson.datatype.jsr310)
        apiWithoutKotlin(rootProject.libs.retrofit)
        apiWithoutKotlin(rootProject.libs.converter.jackson)
        apiWithoutKotlin(rootProject.libs.logging.interceptor)
      }
    }
  }
}

fun gradleProperty(key: String): Provider<String> = providers.gradleProperty(key)

fun FleetKotlinDependencyHandler.apiWithoutKotlin(dependencyNotation: Provider<MinimalExternalModuleDependency>) {
  // TODO: avoid `get().toString()` here and pass provider directly
  api(dependencyNotation.get().toString()) {
    excludeKotlinDeps()
  }
}

fun <T : ModuleDependency> T.excludeKotlinDeps() {
  exclude(module = "kotlin-runtime")
  exclude(module = "kotlin-reflect")
  exclude(module = "kotlin-stdlib")
  exclude(module = "kotlin-stdlib-common")
  exclude(module = "kotlin-stdlib-jdk8")
}
