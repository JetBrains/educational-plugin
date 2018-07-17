package com.jetbrains.edu.learning

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.intellij.GradleConfiguratorBase
import com.jetbrains.edu.learning.intellij.GradleCourseBuilderBase
import com.jetbrains.edu.learning.intellij.JdkProjectSettings
import com.jetbrains.edu.learning.intellij.generation.GradleCourseProjectGenerator

object FakeGradleBasedLanguage : Language("FakeGradleBasedLanguage")

class FakeGradleConfigurator : GradleConfiguratorBase() {

  private val courseBuilder = FakeGradleCourseBuilder()

  override fun getCourseBuilder(): GradleCourseBuilderBase = courseBuilder
  override fun getTestFileName(): String = TEST_FILE_NAME
  override fun isTestFile(file: VirtualFile): Boolean = file.name == testFileName

  override fun getTaskCheckerProvider() = TaskCheckerProvider { task, project ->
    object : TaskChecker<EduTask>(task, project) {
      override fun check(): CheckResult = CheckResult(CheckStatus.Solved, "")
    }
  }

  companion object {
    const val TEST_FILE_NAME = "Tests.kt"
  }
}

class FakeGradleCourseBuilder : GradleCourseBuilderBase() {
  override val buildGradleTemplateName: String = "fake-language-build.gradle.ft"
  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator = FakeGradleCourseProjectGenerator(this, course)
  override fun refreshProject(project: Project) {}
}

class FakeGradleCourseProjectGenerator(
  builder: FakeGradleCourseBuilder,
  course: Course
) : GradleCourseProjectGenerator(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: JdkProjectSettings) {}
}

fun registerFakeGradleConfigurator(disposable: Disposable) {
  val extension = LanguageExtensionPoint<EduConfigurator<*>>()
  extension.language = FakeGradleBasedLanguage.id
  extension.implementationClass = FakeGradleConfigurator::class.java.name
  PlatformTestUtil.registerExtension(ExtensionPointName.create(EduConfigurator.EP_NAME), extension, disposable)
}
