@file:JvmName("CourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.jetbrains.edu.learning.EduConfigurator
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.Course

val Course.configurator: EduConfigurator<*>? get() {
  val language = languageById ?: return null
  return EduConfiguratorManager.forLanguage(language)
}

val Course.sourceDir: String? get() = configurator?.sourceDir
val Course.testDir: String? get() = configurator?.testDir
