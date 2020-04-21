package com.jetbrains.edu.learning.compatibility

import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.plugins.PluginInfo

/**
 * Provides some info about course even if the support of the particular course type is not available.
 *
 * Support of each course type requires some technologies provided by the corresponding plugins.
 * And without these plugins, the corresponding courses cannot be started.
 * For example, Kotlin courses required kotlin, gradle and junit plugins.
 *
 * But if we hide all unavailable (i.e. when required plugins are disabled or not installed) courses from users,
 * they are unlikely to find them or they wouldn't find them.
 * To improve discoverability, the corresponding UI renders unavailable courses but forbids to start them.
 * Also, it uses compatibility providers to shows users what they should do to start a particular course.
 *
 * @see [CoursesPanel]
 * @see [CourseCompatibility]
 * @see [CourseCompatibilityProviderEP]
 */
interface CourseCompatibilityProvider {
  /**
   * Provides info about required IDE plugins for correct work of the corresponding courses.
   * Returns `null` if the corresponding courses are not available in the environment.
   */
  fun requiredPlugins(): List<PluginInfo>?

  /**
   * Display name of the corresponding language or framework
   */
  val technologyName: String
}
