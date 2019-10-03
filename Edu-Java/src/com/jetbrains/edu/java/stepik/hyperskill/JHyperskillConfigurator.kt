package com.jetbrains.edu.java.stepik.hyperskill

import com.intellij.lang.java.JavaLanguage
import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.stepik.fileName
import com.jetbrains.edu.jvm.stepik.hyperskill.HyperskillGradleTaskCheckerProvider
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import javax.swing.JPanel

class JHyperskillConfigurator : GradleConfiguratorBase(), HyperskillConfigurator<JdkProjectSettings> {
  override fun getCourseBuilder() = JHyperskillCourseBuilder()
  override fun getTaskCheckerProvider() = HyperskillGradleTaskCheckerProvider()
  override fun isEnabled(): Boolean = true
  override fun additionalTaskTab(currentTask: Task?, project: Project): Pair<JPanel, String>? {
    return super<HyperskillConfigurator>.additionalTaskTab(currentTask, project)
  }
  override fun getMockFileName(text: String) = fileName(JavaLanguage.INSTANCE, text)
  override fun getTestDirs(): MutableList<String> = mutableListOf("${EduNames.TEST}/stageTest", EduNames.TEST)
}
