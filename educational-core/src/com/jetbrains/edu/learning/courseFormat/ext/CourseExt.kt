@file:JvmName("CourseExt")

package com.jetbrains.edu.learning.courseFormat.ext

import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.LayeredIcon
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.compatibility.CourseCompatibility
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProvider
import com.jetbrains.edu.learning.compatibility.CourseCompatibilityProviderEP
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.JetBrainsAcademyCourse
import javax.swing.Icon

val Course.configurator: EduConfigurator<*>? get() {
  val language = languageById ?: return null
  return EduConfiguratorManager.findConfigurator(itemType, environment, language)
}

val Course.compatibilityProvider: CourseCompatibilityProvider?
  get() {
    return CourseCompatibilityProviderEP.find(languageID, environment)
  }

val Course.sourceDir: String? get() = configurator?.sourceDir
val Course.testDirs: List<String> get() = configurator?.testDirs.orEmpty()

val Course.project: Project? get() {
  for (project in ProjectManager.getInstance().openProjects) {
    if (this == StudyTaskManager.getInstance(project).course) {
      return project
    }
  }
  return null
}

fun Course.getDecoratedLogo(icon: Icon?): Icon? {
  if (icon == null) return null
  return if (compatibility != CourseCompatibility.Compatible) {
    LayeredIcon(2).apply {
      setIcon(icon, 0, 0, 0)
      setIcon(AllIcons.General.ExclMark, 1, JBUI.scale(7), JBUI.scale(7))
    }
  } else {
    visibility.getDecoratedLogo(icon)
  }
}

val Course.hasSections: Boolean get() = sections.isNotEmpty()

val Course.hasTopLevelLessons: Boolean get() = lessons.isNotEmpty()

val Course.allTasks: List<Task> get() {
  val allTasks = mutableListOf<Task>()
  course.visitTasks { allTasks += it }
  return allTasks
}

val Course.languageDisplayName: String get() = languageById?.displayName ?: languageID

val Course.technologyName: String?
  get() = compatibilityProvider?.technologyName ?: languageById?.displayName

val Course.supportedTechnologies: List<String>
  get() {
    return when (this) {
      is JetBrainsAcademyCourse -> this.supportedLanguages
      else -> if (technologyName != null) listOf(technologyName!!) else emptyList()
    }
  }