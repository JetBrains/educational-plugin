package com.jetbrains.edu.learning

import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

class PlainTextConfigurator : EduConfigurator<Unit> {

  override fun getCourseBuilder() = PlainTextCourseBuilder()
  override fun getTestFileName() = "test.txt"
  override fun excludeFromArchive(name: String) = false

  override fun getTaskCheckerProvider() = TaskCheckerProvider { task, project ->
    object : TaskChecker<EduTask>(task, project) {
      override fun check(): CheckResult = CheckResult(CheckStatus.Solved, "")
    }
  }
}

class PlainTextCourseBuilder : EduCourseBuilder<Unit> {
  override fun createTaskContent(project: Project, task: Task, parentDirectory: VirtualFile, course: Course): VirtualFile? = null
  override fun getLanguageSettings(): EduCourseBuilder.LanguageSettings<Unit> = EduCourseBuilder.LanguageSettings { }
  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<Unit> = PlainTextCourseGenerator(this, course)  
}

class PlainTextCourseGenerator(builder: EduCourseBuilder<Unit>, course: Course) : CourseProjectGenerator<Unit>(builder, course)

fun registerPlainTextConfigurator(disposable: Disposable) {
  val extension = LanguageExtensionPoint<EduConfigurator<*>>()
  extension.language = PlainTextLanguage.INSTANCE.id
  extension.implementationClass = PlainTextConfigurator::class.java.name
  PlatformTestUtil.registerExtension(ExtensionPointName.create(EduConfigurator.EP_NAME), extension, disposable)
}
