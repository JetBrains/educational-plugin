package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.checkio.utils.CheckiONames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames
import com.jetbrains.edu.learning.placeholder.PlaceholderPainter
import com.jetbrains.edu.learning.stepik.StepikNames
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_TYPE
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.YAML_TEST_PROJECT_READY
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.YAML_TEST_THROW_EXCEPTION
import com.jetbrains.edu.learning.yaml.YamlTestCase

class YamlTypeChangedTest : YamlTestCase() {

  override fun setUp() {
    super.setUp()
    project.putUserData(YAML_TEST_PROJECT_READY, false)
  }

  override fun createCourse() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("test1.txt", text = "// <p>my</p> task file text") {
            placeholder(0, "TODO")
          }
        }
        choiceTask("choice", choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.CORRECT)) {
          taskFile("test1.txt")
        }
      }
    }
  }

  fun `test edu to choice task`() {
    testTaskTypeChanged(ChoiceTask().itemType, ChoiceTask::class.java)
  }

  // EDU-1907
  fun `test placeholders repainted`() {
    // imitating opening task file and placeholders painting
    val taskFile = findTaskFile(0, 0, "test1.txt")
    myFixture.openFileInEditor(taskFile.getVirtualFile(project)!!)
    PlaceholderPainter.showPlaceholders(project, taskFile)

    // change task type and remove placeholders
    testTaskTypeChanged(ChoiceTask().itemType, ChoiceTask::class.java)

    // check that old placeholders not painted anymore
    assertEquals(0, PlaceholderPainter.getPaintedPlaceholder().size)
  }

  fun `test choice to edu task`() {
    testTaskTypeChanged(EduTask().itemType, EduTask::class.java)
  }

  fun `test lesson to framework lesson`() {
    testLessonTypeChanged("framework", FrameworkLesson::class.java)
  }

  fun `test framework to lesson`() {
    testLessonTypeChanged("lesson", Lesson::class.java)
  }

  fun `test edu to coursera course`() {
    testCourseTypeChanged(CourseraNames.COURSE_TYPE.lowercase(), CourseraCourse::class.java)
  }

  fun `test edu to stepik course`() {
    project.putUserData(YAML_TEST_THROW_EXCEPTION, false)
    testCourseTypeChanged(StepikNames.STEPIK_TYPE.lowercase(), StepikCourse::class.java)
  }

  fun `test edu to hyperskill course`() {
    project.putUserData(YAML_TEST_THROW_EXCEPTION, false)
    testCourseTypeChanged(HYPERSKILL_TYPE.lowercase(), HyperskillCourse::class.java)
  }

  fun `test edu to checkio failed`() {
    project.putUserData(YAML_TEST_THROW_EXCEPTION, false)
    testCourseTypeDidntChange(CheckiONames.CHECKIO_TYPE)
  }

  private fun testCourseTypeDidntChange(type: String) {
    val course = getCourse()
    loadItemFromConfig(course, """
      |type: $type
      |title: Kotlin Course41
      |language: English
      |summary: test
      |programming_language: Plain text
      |content:
      |- lesson1
      |""".trimMargin())

    assertEquals(course.itemType, StudyTaskManager.getInstance(project).course!!.itemType)
  }

  private fun <T : Course> testCourseTypeChanged(courseType: String, expectedCourse: Class<T>) {
    val course = getCourse()
    loadItemFromConfig(course, """
      |type: $courseType
      |title: Kotlin Course41
      |language: English
      |summary: test
      |programming_language: Plain text
      |content:
      |- lesson1
      |""".trimMargin())

    val loadedCourse = getCourse()
    assertInstanceOf(loadedCourse, expectedCourse)
    assertEquals(course.items.size, loadedCourse.items.size)
  }

  private fun <T : Task> testTaskTypeChanged(type: String, expectedClass: Class<T>) {
    val task = findTask(0, 0)
    loadItemFromConfig(task, """
      |type: $type
      |feedback_link: http://example.com
      |files:
      |- name: test1.txt
      |""".trimMargin())

    val loadedTask = findTask(0, 0)
    assertInstanceOf(loadedTask, expectedClass)
    assertEquals(1, loadedTask.index)
    assertEquals(1, loadedTask.taskFiles.size)
  }

  private fun <T : Lesson> testLessonTypeChanged(type: String, expectedClass: Class<T>) {
    val lesson = findLesson(0)
    loadItemFromConfig(lesson, """
      |type: $type
      |content:
      | - task1
      | - choice
      |""".trimMargin())

    val loadedLesson = findLesson(0)
    assertInstanceOf(loadedLesson, expectedClass)
    assertEquals(1, loadedLesson.index)
    assertEquals(2, loadedLesson.items.size)
  }
}