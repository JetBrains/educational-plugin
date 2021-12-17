package com.jetbrains.edu.php

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.php.composer.actions.ComposerAbstractAction

class PhpCourseBuilder : EduCourseBuilder<PhpProjectSettings> {
  override val taskTemplateName: String = PhpConfigurator.TASK_PHP
  override val mainTemplateName: String = PhpConfigurator.MAIN_PHP
  override val testTemplateName: String = PhpConfigurator.TEST_PHP

  override fun getLanguageSettings(): LanguageSettings<PhpProjectSettings> = PhpLanguageSettings()

  override fun getCourseProjectGenerator(course: Course):
    CourseProjectGenerator<PhpProjectSettings> = PhpCourseProjectGenerator(this, course)

  override fun refreshProject(project: Project, cause: RefreshCause) {
    if (cause == RefreshCause.DEPENDENCIES_UPDATED) {
      ComposerAbstractAction.refreshConfigAndLockFiles(project.courseDir, null)
    }
  }
}