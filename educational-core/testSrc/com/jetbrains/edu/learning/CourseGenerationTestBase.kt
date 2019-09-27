package com.jetbrains.edu.learning

import com.intellij.idea.IdeaTestApplication
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.*
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.learning.courseFormat.Course
import java.io.File

// TODO: use [HeavyPlatformTestCase] as base class
abstract class CourseGenerationTestBase<Settings> : UsefulTestCase() {

  abstract val courseBuilder: EduCourseBuilder<Settings>
  abstract val defaultSettings: Settings

  protected lateinit var rootDir: VirtualFile
  protected lateinit var project: Project

  private lateinit var application: IdeaTestApplication

  protected fun findFile(path: String): VirtualFile = rootDir.findFileByRelativePath(path) ?: error("Can't find $path")

  protected fun createCourseStructure(course: Course) {
    val generator = courseBuilder.getCourseProjectGenerator(course) ?: error("given builder returns null as course project generator")
    val project = generator.doCreateCourseProject(rootDir.path, defaultSettings as Any) ?: error("Cannot create project")

    runInEdtAndWait {
      this.project = project
    }
  }

  protected fun generateCourseStructure(pathToCourseJson: String, courseMode: CourseMode = CourseMode.STUDENT): Course {
    val course = createCourseFromJson(pathToCourseJson, courseMode)
    createCourseStructure(course)
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
      RunAll()
        .append(ThrowableRunnable { runWriteAction { rootDir.delete(this) } })
        .append(ThrowableRunnable { LightPlatformTestCase.doTearDown(project, application) })
        .append(ThrowableRunnable {
          // BACKCOMPAT: 2019.2
          @Suppress("DEPRECATION")
          PlatformTestCase.closeAndDisposeProjectAndCheckThatNoOpenProjects(project)
        })
        .run()
    } finally {
      super.tearDown()
    }
  }

}
