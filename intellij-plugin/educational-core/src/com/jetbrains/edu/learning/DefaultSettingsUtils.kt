package com.jetbrains.edu.learning

import org.jetbrains.annotations.NonNls
import java.nio.file.Files
import java.nio.file.Paths

object DefaultSettingsUtils {

  /**
   * Returns [Ok] with path value if system properties contain value for given [property]
   * and the corresponding path exists. Otherwise, returns [Err] with the corresponding error message
   */
  fun findPath(@NonNls property: String, @NonNls name: String): Result<String, String> {
    val path = System.getProperty(property)
      ?: return Err("Failed to find $name because `$property` system property is not provided")
    if (!Files.exists(Paths.get(path))) {
      return Err("`$path` doesn't exist")
    }
    return Ok(path)
  }
}
