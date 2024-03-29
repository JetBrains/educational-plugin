package com.jetbrains.edu.learning

import org.jetbrains.annotations.NonNls
import java.nio.file.Paths
import kotlin.io.path.exists

object DefaultSettingsUtils {

  private const val ENV_PREFIX = "COM_JETBRAINS_EDU_"

  private val NON_WORD_SYMBOL_SEQUENCE_RE = """\W+""".toRegex()

  /**
   * Returns [Ok] with path value if [propertyValue] finds value for given [property]
   * and the corresponding path exists. Otherwise, returns [Err] with the corresponding error message
   */
  fun findPath(@NonNls property: String, @NonNls name: String): Result<String, String> {
    return propertyValue(property, name)
      .flatMap { path ->
        if (!Paths.get(path).exists()) Err("`$path` doesn't exist") else Ok(path)
      }
  }

  /**
   * Returns [Ok] with value for given [property] if system properties contain value with given name
   * or there is the corresponding environment variable.
   * Otherwise, returns [Err] with the corresponding error message
   *
   * Environment variable name constructs from [property] name in the following way:
   * - add `COM_JETBRAINS_EDU_` prefix
   * - replace all non-word symbol sequences with `_`
   * - convert all symbols to the upper case
   *
   * For example, to provide value for `project.python.interpreter` property,
   * environment variable name should be COM_JETBRAINS_EDU_PROJECT_PYTHON_INTERPRETER
   */
  fun propertyValue(@NonNls property: String, @NonNls name: String): Result<String, String> {
    val value = System.getProperty(property) ?:
                System.getenv("$ENV_PREFIX${property.replace(NON_WORD_SYMBOL_SEQUENCE_RE, "_").uppercase()}")
    return if (value != null) Ok(value) else Err("Failed to find $name because `$property` property is not provided")
  }
}
