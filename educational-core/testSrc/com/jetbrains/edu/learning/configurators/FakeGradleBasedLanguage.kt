package com.jetbrains.edu.learning.configurators

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.FileTypeFactory
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
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

class FakeGradleConfigurator : EduConfigurator<Unit> {

  private val courseBuilder = FakeGradleCourseBuilder()

  override fun getSourceDir(): String = EduNames.SRC
  override fun getTestDirs(): List<String> = listOf(EduNames.TEST)

  override fun getCourseBuilder(): FakeGradleCourseBuilder = courseBuilder
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

class FakeGradleCourseBuilder : EduCourseBuilder<Unit> {
  override fun getLanguageSettings(): LanguageSettings<Unit> = object : LanguageSettings<Unit>() {
    override fun getSettings() {}
  }

  override fun getCourseProjectGenerator(course: Course): FakeGradleCourseProjectGenerator = FakeGradleCourseProjectGenerator(
    this, course)
  override fun refreshProject(project: Project) {}
}

class FakeGradleCourseProjectGenerator(
  builder: FakeGradleCourseBuilder,
  course: Course
) : CourseProjectGenerator<Unit>(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: Unit) {}

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile) {
    super.createAdditionalFiles(project, baseDir)
    GeneratorUtils.createChildFile(baseDir, "build.gradle", "")
    GeneratorUtils.createChildFile(baseDir, "settings.gradle", "")
  }
}
