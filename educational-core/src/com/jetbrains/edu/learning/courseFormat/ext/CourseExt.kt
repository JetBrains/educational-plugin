@file:JvmName("CourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.LayeredIcon
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.EduConfigurator
import com.jetbrains.edu.learning.EduConfiguratorManager
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseCompatibility
import javax.swing.Icon

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

val Course.tooltipText: String? get() {
  return if (compatibility != CourseCompatibility.COMPATIBLE) {
    "Course version is incompatible with plugin version"
  } else {
    visibility.tooltipText
  }
}

fun Course.getDecoratedLogo(icon: Icon?): Icon? {
  if (icon == null) return null
  return if (compatibility != CourseCompatibility.COMPATIBLE) {
    LayeredIcon(2).apply {
      setIcon(icon, 0, 0, 0)
      setIcon(AllIcons.General.ExclMark, 1, JBUI.scale(7), JBUI.scale(7))
    }
  } else {
    visibility.getDecoratedLogo(icon)
  }
}
