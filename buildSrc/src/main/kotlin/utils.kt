import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.kotlin.dsl.exclude

fun Project.prop(name: String): String = findProperty(name) as? String ?: error("Property `$name` is not defined in gradle.properties")

fun Project.hasProp(name: String): Boolean = findProperty(name) != null

fun Project.withProp(name: String, action: (String) -> Unit) {
  if (hasProp(name)) {
    action(prop(name))
  }
}

fun <T : ModuleDependency> T.excludeKotlinSerializationDeps() {
  // Kotlin serialization
  exclude(module = "kotlinx-serialization-core-jvm")
  exclude(module = "kotlinx-serialization-json")
}
