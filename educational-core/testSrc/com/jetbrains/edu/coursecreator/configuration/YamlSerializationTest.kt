package com.jetbrains.edu.coursecreator.configuration

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import org.junit.Test


class YamlSerializationTest : EduTestCase() {
  @Test
  fun `test edu task`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>type here\nand here</p>") {
            placeholder(0, "42 is the answer", hints = listOf("hint 1", "hint 2"))
          }
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
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
    |""".trimMargin("|"))
  }

  @Test
  fun `test edu task with dependency`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>test</p>") {
            placeholder(0, "f()")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("Test.java", "<p>type here</p>") {
            placeholder(0, "f()", dependency = "lesson1#task1#Test.java#1")
          }
        }
      }
    }.findTask("lesson2", "task1")
    doTest(task, """
    |type: edu
    |task_files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    dependency:
    |      lesson: lesson1
    |      task: task1
    |      file: Test.java
    |      placeholder: 1
    |      is_visible: true
    |""".trimMargin("|"))
  }

  @Test
  fun `test output task`() {
    val task = course {
      lesson {
        outputTask {
          taskFile("Test.java", "")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: output
    |task_files:
    |- name: Test.java
    |""".trimMargin("|"))
  }

  @Test
  fun `test course`() {
    val course = course {
      lesson("the first lesson")
      lesson("the second lesson")
    }
    course.languageCode = "ru"
    course.description = "This is a course about string theory.\nWhy not?"
    doTest(course, """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?
      |programming_language: Plain text
      |content:
      |- the first lesson
      |- the second lesson
      |
    """.trimMargin("|"))
  }

  @Test
  fun `test lesson`() {
    val lesson = course {
      lesson {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    doTest(lesson, """
      |content:
      |- Introduction Task
      |- Advanced Task
      |
    """.trimMargin("|"))
  }

  @Test
  fun `test section`() {
    val section = course {
      section {
        lesson("Introduction Lesson")
        lesson("Advanced Lesson")
      }
    }.items[0]

    doTest(section, """
      |content:
      |- Introduction Lesson
      |- Advanced Lesson
      |
    """.trimMargin("|"))
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = YamlFormatSynchronizer.MAPPER.writeValueAsString(item)
    assertEquals(expected, actual)
  }
}