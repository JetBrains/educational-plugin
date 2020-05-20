package com.jetbrains.edu.learning

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.HeavyPlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.configurator

abstract class CourseGenerationTestBase<Settings> : HeavyPlatformTestCase() {

  abstract val defaultSettings: Settings

  protected lateinit var rootDir: VirtualFile

  protected fun findFile(path: String): VirtualFile = rootDir.findFileByRelativePath(path) ?: error("Can't find $path")

  protected fun createCourseStructure(course: Course) {
    val configurator = course.configurator ?: error("Failed to find `EduConfigurator` for `${course.name}` course")
    val generator = configurator.courseBuilder.getCourseProjectGenerator(course) ?: error("given builder returns null as course project generator")
    val project = generator.doCreateCourseProject(rootDir.path, defaultSettings as Any) ?: error("Cannot create project")

    runInEdtAndWait {
      myProject = project
    }
  }

  protected fun generateCourseStructure(pathToCourseJson: String, courseMode: CourseMode = CourseMode.STUDENT): Course {
    val course = createCourseFromJson(pathToCourseJson, courseMode)
    createCourseStructure(course)
    return course
  }

  override fun setUp() {
    super.setUp()
    rootDir = tempDir.createTempVDir()
  }

  /**
   * It intentionally does nothing to avoid project creation in [setUp].
   *
   * If you need to create course project, use [createCourseStructure]
   */
  override fun setUpProject() {}
}
