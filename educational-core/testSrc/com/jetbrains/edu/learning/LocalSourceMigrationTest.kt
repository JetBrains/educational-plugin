package com.jetbrains.edu.learning

import com.intellij.openapi.util.io.FileUtil
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.serialization.converter.LANGUAGE_TASK_ROOTS
import com.jetbrains.edu.learning.serialization.converter.TaskRoots
import org.hamcrest.CoreMatchers.*
import org.junit.Assert.assertThat
import java.io.File

class LocalSourceMigrationTest : LightPlatformCodeInsightFixtureTestCase() {

  private val dataFileName: String get() = getTestName(true).trim().replace(" ", "_") + ".json"

  override fun getTestDataPath(): String = "testData/localCourses"

  fun `test to 8 version`() {
    val course = EduUtils.deserializeLocalCourse(loadLocalCourseJsonText()) ?: error("Failed to load local course")
    assert(course.courseType == "Android")
    assert(course.languageID == "kotlin")
  }

  fun `test kotlin sixth version`() {
    val course = EduUtils.deserializeLocalCourse(loadLocalCourseJsonText()) ?: error("Failed to load local course")
    val section = course.items[0] as Section
    val kotlinTaskRoots = LANGUAGE_TASK_ROOTS[EduNames.KOTLIN]
    section.lessons.forEach { it -> checkLesson(it, kotlinTaskRoots) }
    checkLesson(course.items[1] as FrameworkLesson, kotlinTaskRoots)
    checkLesson(course.items[2] as Lesson, kotlinTaskRoots)
  }

  fun `test python sixth version`() {
    val course = EduUtils.deserializeLocalCourse(loadLocalCourseJsonText()) ?: error("Failed to load local course")
    val section = course.items[0] as Section
    val pythonTaskRoots = LANGUAGE_TASK_ROOTS[EduNames.PYTHON]
    section.lessons.forEach { it -> checkLesson(it, pythonTaskRoots) }
    checkLesson(course.items[1] as FrameworkLesson, pythonTaskRoots)
    checkLesson(course.items[2] as Lesson, pythonTaskRoots)
  }

  fun `test remote sixth version`() {
    EduUtils.deserializeLocalCourse(loadLocalCourseJsonText()) ?: error("Failed to load local course")
  }

  private fun checkLesson(lesson: Lesson, taskRoots: TaskRoots?) {
    val (baseSrcPathMatcher, baseTestPathMatcher) = if (taskRoots != null) {
      startsWith("${taskRoots.taskFilesRoot}/") to startsWith("${taskRoots.testFilesRoot}/")
    } else {
      not(containsString("/")) to not(containsString("/"))
    }
    val additionalPathMatcher = if (taskRoots != null) allOf(not(baseSrcPathMatcher), not(baseTestPathMatcher)) else allOf()
    val (srcPathMatcher, testPathMatcher) = if (lesson.isAdditional) {
      additionalPathMatcher to additionalPathMatcher
    } else {
      baseSrcPathMatcher to baseTestPathMatcher
    }

    for (task in lesson.taskList) {
      for ((path, taskFile) in task.taskFiles) {
        assertThat(path, srcPathMatcher)
        assertThat(taskFile.name, srcPathMatcher)

        for (placeholder in taskFile.answerPlaceholders) {
          val dependency = placeholder.placeholderDependency ?: continue
          assertThat(dependency.fileName, srcPathMatcher)
        }
      }

      for ((path, _) in task.testsText) {
        assertThat(path, testPathMatcher)
      }

      for ((path, _) in task.additionalFiles) {
        assertThat(path, additionalPathMatcher)
      }
    }
  }

  private fun loadLocalCourseJsonText(): String = FileUtil.loadFile(File(testDataPath, dataFileName))
}
