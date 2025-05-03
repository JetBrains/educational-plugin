import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.accessors.runtime.addConfiguredDependencyTo
import org.gradle.kotlin.dsl.exclude

fun Project.prop(name: String): String = findProperty(name) as? String ?: error("Property `$name` is not defined in gradle.properties")

fun Project.hasProp(name: String): Boolean = findProperty(name) != null

fun Project.withProp(name: String, action: (String) -> Unit) {
  if (hasProp(name)) {
    action(prop(name))
  }
}

fun DependencyHandler.implementationWithoutKotlin(dependencyNotation: Provider<*>) {
  implementation(dependencyNotation) {
    excludeKotlinDeps()
  }
}

fun DependencyHandler.testImplementationWithoutKotlin(dependencyNotation: Provider<*>) {
  testImplementation(dependencyNotation) {
    excludeKotlinDeps()
  }
}

// Is it possible to import `implementation` extension from "stdlib"?
private fun DependencyHandler.implementation(
  dependencyNotation: Provider<*>,
  dependencyConfiguration: Action<ExternalModuleDependency>
) = addConfiguredDependencyTo(
  this, "implementation", dependencyNotation, dependencyConfiguration
)

// Is it possible to import `testImplementation` extension from "stdlib"?
private fun DependencyHandler.testImplementation(
  dependencyNotation: Provider<*>,
  dependencyConfiguration: Action<ExternalModuleDependency>
) = addConfiguredDependencyTo(
  this, "testImplementation", dependencyNotation, dependencyConfiguration
)

fun <T : ModuleDependency> T.excludeKotlinDeps() {
  // Kotlin stdlib
  exclude(module = "kotlin-runtime")
  exclude(module = "kotlin-reflect")
  exclude(module = "kotlin-stdlib")
  exclude(module = "kotlin-stdlib-common")
  exclude(module = "kotlin-stdlib-jdk8")
  exclude(module = "kotlin-stdlib-jdk7")
  // Kotlin coroutines
  exclude(module = "kotlinx-coroutines-core")
  exclude(module = "kotlinx-coroutines-core-jvm")
  exclude(module = "kotlinx-coroutines-jdk8")
  exclude(module = "kotlinx-coroutines-slf4j")
}

fun <T : ModuleDependency> T.excludeKotlinSerializationDeps() {
  // Kotlin serialization
  exclude(module = "kotlinx-serialization-core-jvm")
  exclude(module = "kotlinx-serialization-json")
}
