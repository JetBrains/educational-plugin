package com.jetbrains.edu.coursecreator.configuration

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.courseFormat.AnswerPlaceholder
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import org.junit.Assert.assertEquals
import org.junit.Test


class YamlSerializationTest {
  @Test
  fun `test edu task`() {
    val eduTask = EduTask()
    val taskFile = TaskFile()
    taskFile.name = "Test.java"
    eduTask.addTaskFile(taskFile)
    taskFile.createPlaceholder(0, "type here\nand here", listOf("hint 1", "hint 2"), "42 is the answer")
    val taskInfo = CourseInfoSynchronizer.getMapper().writeValueAsString(eduTask)
    val expectedConfig = """
    |type: edu
    |task_files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: |-
    |      type here
    |      and here
    |    hints:
    |    - hint 1
    |    - hint 2
    |""".trimMargin("|")
    assertEquals(expectedConfig, taskInfo)
  }

  @Test
  fun `test output task`() {
    val outputTask = OutputTask()
    val taskFile = TaskFile()
    taskFile.name = "Test.java"
    outputTask.addTaskFile(taskFile)
    val taskInfo = CourseInfoSynchronizer.getMapper().writeValueAsString(outputTask)
    val expectedConfig = """
    |type: output
    |task_files:
    |- name: Test.java
    |""".trimMargin("|")
    assertEquals(expectedConfig, taskInfo)
  }

  @Test
  fun `test course`() {
    val course = Course()
    val lesson1 = Lesson()
    lesson1.name = "the first lesson"
    val lesson2 = Lesson()
    lesson2.name = "the second lesson"
    course.apply {
      name = "Test Course"
      description = "This is a course about string theory.\nWhy not?"
      language = PlainTextLanguage.INSTANCE.id
      languageCode = "ru"
      items = listOf(lesson1, lesson2)
    }
    val expectedCourseInfo = """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?
      |programming_language: Plain text
      |lessons:
      |- the first lesson
      |- the second lesson
      |
    """.trimMargin("|")
    val actualCourseInfo = CourseInfoSynchronizer.getMapper().writeValueAsString(course)
    assertEquals(expectedCourseInfo, actualCourseInfo)
  }

  @Test
  fun `test lesson`() {
    val lesson = Lesson()
    val task1 = EduTask()
    task1.name = "Introduction task"
    val task2 = EduTask()
    task2.name = "Advanced task"
    lesson.addTask(task1)
    lesson.addTask(task2)
    val expectedTaskInfo = """
      |tasks:
      |- Introduction task
      |- Advanced task
      |
    """.trimMargin("|")
    val actualTaskInfo = CourseInfoSynchronizer.getMapper().writeValueAsString(lesson)
    assertEquals(expectedTaskInfo, actualTaskInfo)
  }

  private fun TaskFile.createPlaceholder(offset: Int, taskText: String, hints: List<String>, possibleAnswer: String,
                                 isStudyMode: Boolean = this.task?.lesson?.course?.isStudy ?: false): AnswerPlaceholder {
    val placeholder = AnswerPlaceholder()
    placeholder.taskFile = this
    placeholder.offset = offset
    placeholder.length = taskText.length
    placeholder.placeholderText = taskText
    placeholder.hints = hints
    placeholder.useLength = isStudyMode
    placeholder.possibleAnswer = possibleAnswer
    this.answerPlaceholders.add(placeholder)
    return placeholder
  }
}