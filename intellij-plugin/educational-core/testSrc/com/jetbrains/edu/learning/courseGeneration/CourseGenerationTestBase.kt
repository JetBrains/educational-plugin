package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.learning.EduHeavyTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.createCourseFromJson
import com.jetbrains.edu.learning.newproject.EduProjectSettings

@Suppress("UnstableApiUsage")
abstract class CourseGenerationTestBase<Settings : EduProjectSettings> : EduHeavyTestCase(), CourseGenerationTestMixin<Settings> {

  override fun tearDown() = invokeAndWaitIfNeeded {
    super.tearDown()
  }

  override val rootDir: VirtualFile by lazy { tempDir.createVirtualDir() }

  protected fun findFile(path: String): VirtualFile = rootDir.findFileByRelativePath(path) ?: error("Can't find $path")

  override fun createCourseStructure(
    course: Course,
    metadata: Map<String, String>,
    waitForProjectConfiguration: Boolean
  ): Project {
    val project = super.createCourseStructure(course, metadata, waitForProjectConfiguration)
    runInEdtAndWait {
      myProject = project
    }
    return project
  }

  protected fun generateCourseStructure(pathToCourseJson: String, courseMode: CourseMode = CourseMode.STUDENT): Course {
    val course = createCourseFromJson(pathToCourseJson, courseMode)
    createCourseStructure(course)
    return course
  }

  /**
   * It intentionally does nothing to avoid project creation in [setUp].
   *
   * If you need to create a course project, use [createCourseStructure]
   */
  override fun setUpProject() {}
}
