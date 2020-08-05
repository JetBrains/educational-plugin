package com.jetbrains.edu.learning.configurators

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.EduCourseBuilder
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.LanguageSettings
import com.jetbrains.edu.learning.RefreshCause
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.configuration.EduConfigurator
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.hyperskill.HyperskillConfigurator
import javax.swing.Icon

object FakeGradleBasedLanguage : Language("FakeGradleBasedLanguage")

object FakeGradleFileType : LanguageFileType(FakeGradleBasedLanguage) {
  override fun getIcon(): Icon? = null
  override fun getName(): String = "FakeGradleFileType"
  override fun getDefaultExtension(): String = "kt"
  override fun getDescription(): String = "File type for fake gradle based language"
}

class FakeGradleConfigurator : EduConfigurator<Unit> {
  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val courseBuilder: FakeGradleCourseBuilder
    get() = FakeGradleCourseBuilder()

  override val testFileName: String
    get() = TEST_FILE_NAME

  override fun getMockFileName(text: String): String = TASK_FILE_NAME

  override val taskCheckerProvider
    get() = object : TaskCheckerProvider {
      override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
        return object : TaskChecker<EduTask>(task, project) {
          override fun check(indicator: ProgressIndicator): CheckResult = CheckResult(CheckStatus.Solved, "")
        }
      }
    }

  companion object {
    const val TEST_FILE_NAME = "Tests.kt"
    const val TASK_FILE_NAME = "Task.kt"
  }
}

class FakeGradleHyperskillConfigurator : HyperskillConfigurator<Unit>(FakeGradleConfigurator())

class FakeGradleCourseBuilder : EduCourseBuilder<Unit> {
  override fun getLanguageSettings(): LanguageSettings<Unit> = object : LanguageSettings<Unit>() {
    override fun getSettings() {}
  }

  override fun getCourseProjectGenerator(course: Course): FakeGradleCourseProjectGenerator = FakeGradleCourseProjectGenerator(
    this, course)
  override fun refreshProject(project: Project, cause: RefreshCause) {}
  override val mainTemplateName: String? = "Main.kt"
  override val testTemplateName: String? = "Tests.kt"
}

class FakeGradleCourseProjectGenerator(
  builder: FakeGradleCourseBuilder,
  course: Course
) : CourseProjectGenerator<Unit>(builder, course) {
  override fun afterProjectGenerated(project: Project, projectSettings: Unit) {}

  override fun createAdditionalFiles(project: Project, baseDir: VirtualFile, isNewCourse: Boolean) {
    super.createAdditionalFiles(project, baseDir, isNewCourse)
    GeneratorUtils.createChildFile(baseDir, "build.gradle", "")
    GeneratorUtils.createChildFile(baseDir, "settings.gradle", "")
  }
}
