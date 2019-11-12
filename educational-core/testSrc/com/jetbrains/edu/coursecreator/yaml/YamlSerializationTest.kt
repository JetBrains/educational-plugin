package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.FeedbackLink
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.util.*


class YamlSerializationTest : YamlTestCase() {

  fun `test edu task`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "type here\nand here")
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
    |""".trimMargin())
  }

  fun `test empty placeholder`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "")
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
    |    placeholder_text: ""
    |""".trimMargin())
  }

  fun `test placeholder starts with spaces`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "   type here")
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
    |    placeholder_text: '   type here'
    |""".trimMargin())
  }

  fun `test placeholder ends with spaces`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "type here   ")
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
    |    placeholder_text: 'type here   '
    |""".trimMargin())
  }

  fun `test edu task with test files`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Task.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "type here\nand here")
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
    |- name: Test.java
    |  visible: false
    |""".trimMargin())
  }

  fun `test edu task with additional files`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "type here\nand here")
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
    |- name: Additional.java
    |  visible: false
    |""".trimMargin())
  }

  fun `test edu task with dependency`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>f()</p>") {
            placeholder(0, "test")
          }
        }
      }
      lesson {
        eduTask {
          taskFile("Test.java", "<p>f()</p>") {
            placeholder(0, placeholderText = "type here", dependency = "lesson1#task1#Test.java#1")
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
    |""".trimMargin())
  }

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
    |""".trimMargin())
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
      |message_correct: Congratulations!
      |message_incorrect: Incorrect solution
      |files:
      |- name: Test.java
      |  visible: true
      |""".trimMargin())
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
      |message_correct: Congratulations!
      |message_incorrect: Incorrect solution
      |files:
      |- name: Test.java
      |  visible: true
      |""".trimMargin())
  }

  fun `test course`() {
    val course = course(courseMode = CCUtils.COURSE_MODE) {
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
    """.trimMargin())
  }

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
    """.trimMargin())
  }

  fun `test lesson with custom presentable name`() {
    val lesson = course {
      lesson {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    lesson.customPresentableName = "my new lesson"
    @Suppress("DEPRECATION") // using `customPresentableName` here is ok
    doTest(lesson, """
      |custom_name: ${lesson.customPresentableName}
      |content:
      |- Introduction Task
      |- Advanced Task
      |
    """.trimMargin())
  }

  fun `test use dir name for lesson with custom name`() {
    val course = course {
      section {
        lesson {
          eduTask("Introduction Task")
          eduTask("Advanced Task")
        }
      }
    }
    val section = course.sections.first()
    val lesson = section.lessons.first()
    lesson.customPresentableName = "my new lesson"

    doTest(section, """
      |content:
      |- ${lesson.name}
      |
    """.trimMargin())
  }

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
    """.trimMargin())
  }

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
    """.trimMargin())
  }

  fun `test section with custom name`() {
    val section = course {
      section {
        lesson("Introduction Lesson")
        lesson("Advanced Lesson")
      }
    }.items[0]

    section.customPresentableName = "custom section name"
    doTest(section, """
      |custom_name: custom section name
      |content:
      |- Introduction Lesson
      |- Advanced Lesson
      |
    """.trimMargin())
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
    |""".trimMargin())
  }

  fun `test with custom presentable name`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
      }
    }.findTask("lesson1", "task1")
    val taskCustomName = "task custom name"
    task.customPresentableName = taskCustomName
    doTest(task, """
    |type: edu
    |custom_name: $taskCustomName
    |""".trimMargin())
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
      |summary: Test Course Description
      |programming_language: Plain text
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin())
  }

  fun `test course with hidden solutions`() {
    val course = course {}
    course.solutionsHidden = true
    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |solutions_hidden: true
      |
    """.trimMargin())
  }

  fun `test task with hidden solution`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {}
      }
    }.findTask("lesson1", "task1")
    val taskCustomName = "task custom name"
    task.customPresentableName = taskCustomName
    task.solutionHidden = true
    doTest(task, """
    |type: edu
    |custom_name: $taskCustomName
    |solution_hidden: true
    |""".trimMargin("|"))
  }

  fun `test task with hidden solution = false`() {
    val task = course(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {}
      }
    }.findTask("lesson1", "task1")
    val taskCustomName = "task custom name"
    task.customPresentableName = taskCustomName
    task.solutionHidden = false
    doTest(task, """
    |type: edu
    |custom_name: $taskCustomName
    |solution_hidden: false
    |""".trimMargin("|"))
  }

  fun `test empty lesson`() {
    val lesson = course {
      lesson {
      }
    }.items.first()

    doTest(lesson, """
      |{}
      |
    """.trimMargin())
  }

  fun `test empty section`() {
    val section = course {
      section {
      }
    }.items.first()

    doTest(section, """
      |{}
      |
    """.trimMargin())
  }

  fun `test empty course`() {
    val course = course(courseMode = CCUtils.COURSE_MODE) {}

    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |
    """.trimMargin())
  }

  fun `test course with lang version`() {
    val course = course(courseMode = CCUtils.COURSE_MODE) {}
    course.languageCode = "ru"
    course.description = "sum"
    course.language = "${PlainTextLanguage.INSTANCE.id} 1.42"
    doTest(course, """
      |title: Test Course
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |
    """.trimMargin())
  }

  fun `test coursera course`() {
    val course = course(courseMode = CCUtils.COURSE_MODE, courseProducer = ::CourseraCourse) {}
    course.languageCode = "ru"
    course.description = "sum"
    course.language = "${PlainTextLanguage.INSTANCE.id} 1.42"
    doTest(course, """
      |type: coursera
      |title: Test Course
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |
    """.trimMargin())
  }

  fun `test coursera course manual submit`() {
    val course = course(courseMode = CCUtils.COURSE_MODE, courseProducer = ::CourseraCourse) {} as CourseraCourse
    course.languageCode = "ru"
    course.description = "sum"
    course.submitManually = true
    course.language = "${PlainTextLanguage.INSTANCE.id} 1.42"
    doTest(course, """
      |type: coursera
      |title: Test Course
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |submit_manually: true
      |
    """.trimMargin())
  }

  fun `test course with non-english locale`() {
    val defaultLocale = Locale.getDefault()
    Locale.setDefault(Locale.KOREAN)

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
    """.trimMargin())

    Locale.setDefault(defaultLocale)
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = YamlFormatSynchronizer.MAPPER.writeValueAsString(item)
    assertEquals(expected, actual)
  }
}