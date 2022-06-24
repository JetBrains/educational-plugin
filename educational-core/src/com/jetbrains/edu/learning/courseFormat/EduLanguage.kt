package com.jetbrains.edu.learning.courseFormat

import org.jetbrains.annotations.NonNls

data class EduLanguage(
  val id: String,
  val version: String = DEFAULT_VERSION
) {

  override fun toString(): String {
    return "$id $version".trimEnd()
  }

  companion object {
    @NonNls
    private const val DEFAULT_VERSION = ""

    /**
     * @param programmingLanguage is string that represents programming language;
     * it could contain only language or language with version (optional),
     * where language is id of [Language];
     * if it's language with version - it's 2 parts with space in between,
     * second one describes version of this language
     * e.g., "JAVA 11", "Python 3.x", etc
     *
     * see [com.jetbrains.edu.learning.EduNames.PYTHON_2_VERSION], [com.jetbrains.edu.learning.EduNames.PYTHON_3_VERSION]
     */
    @JvmStatic
    fun get(programmingLanguage: String): EduLanguage {
      val result = programmingLanguage.trim().split(" ")
      return EduLanguage(result.first(), result.getOrElse(1) { DEFAULT_VERSION })
    }
  }
}
