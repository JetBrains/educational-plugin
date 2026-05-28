package com.jetbrains.edu.java

import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import javax.swing.Icon

class JConfigurator : GradleConfiguratorBase() {
  override val courseBuilder: GradleCourseBuilderBase
    get() = JCourseBuilder()

  override val testFileName: String
    get() = TEST_JAVA

  override val isEnabled: Boolean
    get() = !EduUtilsKt.isAndroidStudio()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = GradleTaskCheckerProvider()

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Java

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val TEST_JAVA = "Tests.java"
    const val TASK_JAVA = "Task.java"
    const val MAIN_JAVA = "Main.java"
  }
}