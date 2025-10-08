package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.edu.learning.EduHeavyTestCase
import com.jetbrains.edu.learning.FileTree
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.Lesson
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

  protected fun checkCourseStructure(course: Course, courseFromServer: Course, expectedFileTree: FileTree) {
    assertEquals("Lessons number mismatch", courseFromServer.lessons.size, course.lessons.size)
    assertEquals("Sections number mismatch", courseFromServer.sections.size, course.sections.size)

    for ((section, newSection) in course.sections.zip(courseFromServer.sections)) {
      assertEquals("""Lesson number mismatch. Lesson name - "${section.name}"""", newSection.lessons.size, section.lessons.size)
      checkLessons(section.lessons, newSection.lessons)
    }

    checkLessons(course.lessons, courseFromServer.lessons)
    expectedFileTree.assertEquals(rootDir)
  }

  private fun checkLessons(
    lessons: List<Lesson>,
    lessonsFromServer: List<Lesson>
  ) {
    for ((lesson, newLesson) in lessons.zip(lessonsFromServer)) {
      assertEquals("""Tasks number mismatch. Lesson name - "${lesson.name}"""", newLesson.taskList.size, lesson.taskList.size)

      assertEquals("Lesson name mismatch", newLesson.name, lesson.name)
      for ((task, newTask) in lesson.taskList.zip(newLesson.taskList)) {
        assertEquals(
          """Task files number mismatch. Lesson name - "${lesson.name}". Task name - "${task.name}".""",
          newTask.taskFiles.size,
          task.taskFiles.size,
        )

        assertEquals(
          """Task text mismatch. Lesson name - "${lesson.name}". Task name - "${task.name}".""",
          newTask.descriptionText,
          task.descriptionText
        )

        assertEquals("Lesson index mismatch", newLesson.index, lesson.index)
      }
    }
  }
}
