package com.jetbrains.edu.java

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator

class JCourseProjectGenerator(builder: GradleCourseBuilderBase,course: Course) : GradleCourseProjectGenerator(builder, course) {

  override fun afterProjectGenerated(project: Project, projectSettings: JdkProjectSettings) {
    super.afterProjectGenerated(project, projectSettings)
    val indentOptions = CodeStyleSettingsManager.getInstance().currentSettings.
                                                                  getCommonSettings(JavaLanguage.INSTANCE).indentOptions
    indentOptions?.TAB_SIZE = 2
    indentOptions?.INDENT_SIZE = 2
  }
}
