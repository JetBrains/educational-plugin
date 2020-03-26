package com.jetbrains.edu.learning.compatibility

class RsCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<String>? {
    return listOf(
      "org.rust.lang",
      "org.toml.lang"
    )
  }
}
