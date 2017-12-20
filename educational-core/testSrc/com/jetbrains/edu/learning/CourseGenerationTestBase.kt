package com.jetbrains.edu.learning

import com.intellij.idea.IdeaTestApplication
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.learning.courseFormat.Course
import java.io.File

abstract class CourseGenerationTestBase<Settings> : UsefulTestCase() {

  abstract val courseBuilder: EduCourseBuilder<Settings>
  abstract val defaultSettings: Settings

  protected lateinit var rootDir: VirtualFile
  protected lateinit var project: Project

  private lateinit var application: IdeaTestApplication

  protected fun findFile(path: String): VirtualFile = rootDir.findFileByRelativePath(path) ?: error("Can't find $path")

  protected fun <Settings> createCourseStructure(builder: EduCourseBuilder<Settings>, course: Course, settings: Settings) {
    val generator = builder.getCourseProjectGenerator(course) ?: error("given builder returns null as course project generator")
    generator.doCreateCourseProject(rootDir.path, settings!!)

    runInEdtAndWait {
      project = ProjectManager.getInstance().openProjects.firstOrNull() ?: error("Cannot find project")
    }
  }

  protected fun generateCourseStructure(pathToCourseJson: String): Course {
    val course = createCourseFromJson(pathToCourseJson)
    createCourseStructure(courseBuilder, course, defaultSettings)
    return course
  }

  override fun setUp() {
    super.setUp()

    application = LightPlatformTestCase.initApplication()

    val tempDir = File(FileUtil.getTempDirectory())
    tempDir.mkdirs()

    rootDir = VfsUtil.findFileByIoFile(tempDir, true) ?: error("Can't find ${tempDir.absolutePath}")
  }

  override fun tearDown() {
    try {
      ApplicationManager.getApplication().runWriteAction { rootDir.delete(this) }

      LightPlatformTestCase.doTearDown(project, application)
    } finally {
      super.tearDown()
    }
  }

}
