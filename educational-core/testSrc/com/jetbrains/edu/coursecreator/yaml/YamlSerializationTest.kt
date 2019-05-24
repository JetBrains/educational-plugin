package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.Experiments
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import org.junit.Test


class YamlSerializationTest : EduTestCase() {
  override fun setUp() {
    super.setUp()
    Experiments.setFeatureEnabled(EduExperimentalFeatures.YAML_FORMAT, true)
  }

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
    |files:
    |- name: Test.java
    |  visible: true
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

  fun `test edu task with test files`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Task.java", "<p>type here\nand here</p>") {
            placeholder(0, "42 is the answer", hints = listOf("hint 1", "hint 2"))
          }
          taskFile("Test.java", "my test", false)
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: edu
    |files:
    |- name: Task.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: |-
    |      type here
    |      and here
    |    hints:
    |    - hint 1
    |    - hint 2
    |- name: Test.java
    |  visible: false
    |""".trimMargin("|"))
  }

  fun `test edu task with additional files`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>type here\nand here</p>") {
            placeholder(0, "42 is the answer", hints = listOf("hint 1", "hint 2"))
          }
          taskFile("Additional.java", "", false)
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: edu
    |files:
    |- name: Test.java
    |  visible: true
    |  placeholders:
    |  - offset: 0
    |    length: 16
    |    placeholder_text: |-
    |      type here
    |      and here
    |    hints:
    |    - hint 1
    |    - hint 2
    |- name: Additional.java
    |  visible: false
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
    |files:
    |- name: Test.java
    |  visible: true
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
    |files:
    |- name: Test.java
    |  visible: true
    |""".trimMargin("|"))
  }

  fun `test quiz task`() {
    val task = course {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT)) {
          taskFile("Test.java", "")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
      |type: choice
      |is_multiple_choice: false
      |options:
      |- text: 1
      |  is_correct: true
      |- text: 2
      |  is_correct: false
      |files:
      |- name: Test.java
      |  visible: true
      |""".trimMargin("|"))
  }

  fun `test quiz task without answers`() {
    val task = course {
      lesson {
        choiceTask(choiceOptions = mapOf("1" to ChoiceOptionStatus.UNKNOWN, "2" to ChoiceOptionStatus.UNKNOWN)) {
          taskFile("Test.java", "")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
      |type: choice
      |is_multiple_choice: false
      |options:
      |- text: 1
      |- text: 2
      |files:
      |- name: Test.java
      |  visible: true
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
  fun `test framework lesson`() {
    val lesson = course {
      frameworkLesson {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    doTest(lesson, """
      |type: framework
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

  fun `test feedback link`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
      }
    }.findTask("lesson1", "task1")
    task.feedbackLink = FeedbackLink()
    task.feedbackLink.link = "example.com"
    doTest(task, """
    |type: edu
    |feedback_link: example.com
    |""".trimMargin("|"))
  }

  fun `test course with environment`() {
    val course = course(courseMode = CCUtils.COURSE_MODE, environment = EduNames.ANDROID) {
      lesson {
        eduTask { }
      }
    }
    doTest(course, """
      |title: Test Course
      |language: English
      |programming_language: Plain text
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|"))
  }

  fun `test empty lesson`() {
    val lesson = course {
      lesson {
      }
    }.items.first()

    doTest(lesson, """
      |{}
      |
    """.trimMargin("|"))
  }

  fun `test empty section`() {
    val section = course {
      section {
      }
    }.items.first()

    doTest(section, """
      |{}
      |
    """.trimMargin("|"))
  }

  fun `test empty course`() {
    val course = course {}

    doTest(course, """
      |title: Test Course
      |language: English
      |programming_language: Plain text
      |
    """.trimMargin("|"))
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = YamlFormatSynchronizer.MAPPER.writeValueAsString(item)
    assertEquals(expected, actual)
  }
}