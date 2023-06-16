package com.jetbrains.edu.javascript.learning

import com.intellij.lang.javascript.ui.NodeModuleNamesUtil.PACKAGE_JSON
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class JsCourseBuilder : EduCourseBuilder<JsNewProjectSettings> {
  override fun taskTemplateName(course: Course): String = JsConfigurator.TASK_JS
  override fun testTemplateName(course: Course): String = JsConfigurator.TEST_JS

  override fun getLanguageSettings(): LanguageSettings<JsNewProjectSettings> = JsLanguageSettings()
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<JsNewProjectSettings> =
    JsCourseProjectGenerator(this, course)

  override fun refreshProject(project: Project, cause: RefreshCause) {
    if (cause == RefreshCause.DEPENDENCIES_UPDATED) {
      val baseDir = project.course?.getDir(project.courseDir) ?: return
      val packageJson = baseDir.findChild(PACKAGE_JSON) ?: return
      installNodeDependencies(project, packageJson)
    }
  }
}
