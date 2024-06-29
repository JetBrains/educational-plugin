package com.jetbrains.edu.java

import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.jvm.gradle.GradleConfiguratorBase
import com.jetbrains.edu.jvm.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.jvm.gradle.checker.GradleTaskCheckerProvider
import com.jetbrains.edu.jvm.stepik.fileName
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
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

  override fun getMockFileName(course: Course, text: String): String = fileName(JavaLanguage.INSTANCE, text)

  override val mockTemplate: String
    get() = getInternalTemplateText(MOCK_JAVA)

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Java

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val TEST_JAVA = "Tests.java"
    const val TASK_JAVA = "Task.java"
    const val MAIN_JAVA = "Main.java"
    const val MOCK_JAVA = "Mock.java"
  }
}