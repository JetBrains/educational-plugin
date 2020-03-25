package com.jetbrains.edu.learning.compatibility

/**
 * @see [CourseCompatibilityProviderEP]
 */
interface CourseCompatibilityProvider {
  /**
   * Provide IDE plugin ids which are required for correct work of the corresponding courses.
   * Returns `null` if the corresponding courses are not available in the environment.
   *
   * @return list of plugin ids
   */
  fun requiredPlugins(): List<String>?
}
