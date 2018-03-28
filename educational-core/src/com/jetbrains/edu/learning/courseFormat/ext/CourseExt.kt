@file:JvmName("CourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.learning.EduConfigurator
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course

val Course.configurator: EduConfigurator<*>? get() {
  val language = languageById ?: return null
  return EduConfiguratorManager.forLanguage(language)
}

val Course.sourceDir: String? get() = configurator?.sourceDir
val Course.testDir: String? get() = configurator?.testDir

val Course.project: Project? get() {
  for (project in ProjectManager.getInstance().openProjects) {
    if (this == StudyTaskManager.getInstance(project).course) {
      return project
    }
  }
  return null
}
