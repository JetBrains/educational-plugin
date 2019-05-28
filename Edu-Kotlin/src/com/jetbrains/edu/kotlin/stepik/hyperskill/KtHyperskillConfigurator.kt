package com.jetbrains.edu.kotlin.stepik.hyperskill

import com.intellij.openapi.project.Project
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.generation.GradleCourseProjectGenerator
import com.jetbrains.edu.jvm.stepik.hyperskill.HyperskillGradleCourseProjectGenerator
import com.jetbrains.edu.jvm.stepik.hyperskill.HyperskillGradleTaskCheckerProvider
import com.jetbrains.edu.kotlin.KtConfigurator
import com.jetbrains.edu.kotlin.KtCourseBuilder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import javax.swing.JPanel

class KtHyperskillConfigurator : GradleConfiguratorBase(), HyperskillConfigurator<JdkProjectSettings> {
  override fun getCourseBuilder() = object: KtCourseBuilder() {
      override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator {
        return HyperskillGradleCourseProjectGenerator(this, course)
      }
  }
  override fun getTaskCheckerProvider() = HyperskillGradleTaskCheckerProvider()
  override fun additionalTaskTab(currentTask: Task?, project: Project): Pair<JPanel, String>? {
    return super<HyperskillConfigurator>.additionalTaskTab(currentTask, project)
  }
  override fun getMockFileName(text: String): String = KtConfigurator.TASK_KT
}
