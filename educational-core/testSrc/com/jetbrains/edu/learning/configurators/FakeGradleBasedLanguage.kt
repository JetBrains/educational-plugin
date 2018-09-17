package com.jetbrains.edu.learning.configurators

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.gradle.GradleConfiguratorBase
import com.jetbrains.edu.learning.gradle.GradleCourseBuilderBase
import com.jetbrains.edu.learning.gradle.JdkProjectSettings
import com.jetbrains.edu.learning.gradle.generation.GradleCourseProjectGenerator
import javax.swing.Icon

object FakeGradleBasedLanguage : Language("FakeGradleBasedLanguage")

object FakeGradleFileType : LanguageFileType(FakeGradleBasedLanguage) {
  override fun getIcon(): Icon? = null
  override fun getName(): String = FakeGradleBasedLanguage.displayName
  override fun getDefaultExtension(): String = "kt"
  override fun getDescription(): String = "File type for fake gradle based language"
}

class FakeGradleTypeFactory : FileTypeFactory() {
  override fun createFileTypes(consumer: FileTypeConsumer) {
    consumer.consume(FakeGradleFileType)
  }
}

class FakeGradleConfigurator : GradleConfiguratorBase() {

  private val courseBuilder = FakeGradleCourseBuilder()

  override fun getCourseBuilder(): GradleCourseBuilderBase = courseBuilder
  override fun getTestFileName(): String = TEST_FILE_NAME

  override fun getTaskCheckerProvider() = TaskCheckerProvider { task, project ->
    object : TaskChecker<EduTask>(task, project) {
      override fun check(indicator: ProgressIndicator): CheckResult = CheckResult(CheckStatus.Solved, "")
    }
  }

  companion object {
    const val TEST_FILE_NAME = "Tests.kt"
  }
}

class FakeGradleCourseBuilder : GradleCourseBuilderBase() {
  override val buildGradleTemplateName: String = "fake-language-build.gradle"
  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator = FakeGradleCourseProjectGenerator(
    this, course)
  override fun refreshProject(project: Project) {}
}

class FakeGradleCourseProjectGenerator(
  builder: FakeGradleCourseBuilder,
  course: Course
) : GradleCourseProjectGenerator(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: JdkProjectSettings) {}
}