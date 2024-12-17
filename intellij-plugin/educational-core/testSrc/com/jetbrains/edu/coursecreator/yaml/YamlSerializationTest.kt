package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikCourse
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer.mapper
import com.jetbrains.edu.learning.yaml.YamlMapper
import com.jetbrains.edu.learning.yaml.YamlMapper.CURRENT_YAML_VERSION
import com.jetbrains.edu.learning.yaml.YamlTestCase
import org.junit.Test
import java.util.*


class YamlSerializationTest : YamlTestCase() {

  @Test
  fun `test edu task`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test remote edu task`() {
    val task = course {
      lesson {
        remoteEduTask {
          taskFile("Test.java")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: remote_edu
    |files:
    |- name: Test.java
    |  visible: true
    |  learner_created: false
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  @Test
  fun `test remote edu task with check profile`() {
    val checkProfile = "hyperskill_go"
    val task = course {
      lesson {
        remoteEduTask(checkProfile = checkProfile) {
          taskFile("Main.go")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: remote_edu
    |files:
    |- name: Main.go
    |  visible: true
    |  learner_created: false
    |check_profile: $checkProfile
    |status: Unchecked
    |record: -1
    |""".trimMargin())
  }

  @Test
  fun `test edu task with content tags`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("Test.java", "<p>42 is the answer</p>") {
            placeholder(0, placeholderText = "type here\nand here")
          }
        }
      }
    }.findTask("lesson1", "task1")
    task.contentTags = listOf("kotlin", "cycles")
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
    |tags:
    |- kotlin
    |- cycles
    |""".trimMargin())
  }

  @Test
  fun `test empty placeholder`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test placeholder starts with spaces`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test placeholder ends with spaces`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test edu task with test files`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test edu task with additional files`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test edu task with dependency`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
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
    |""".trimMargin())
  }

  @Test
  fun `test edu task with files with turned off highlighting`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("A.java", "a java") {
            withHighlightLevel(EduFileErrorHighlightLevel.NONE)
          }
          taskFile("B.java", "b java") {
            withHighlightLevel(EduFileErrorHighlightLevel.ALL_PROBLEMS)
          }
          taskFile("C.java", "c java") {
            withHighlightLevel(EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION)
          }
          taskFile("D.java", "d java")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(task, """
    |type: edu
    |files:
    |- name: A.java
    |  visible: true
    |  highlight_level: NONE
    |- name: B.java
    |  visible: true
    |- name: C.java
    |  visible: true
    |- name: D.java
    |  visible: true
    |""".trimMargin())
  }

  @Test
  fun `test output task`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test quiz task`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
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
      |local_check: true
      |""".trimMargin())
  }

  @Test
  fun `test quiz task without answers`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
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
      |local_check: true
      |""".trimMargin())
  }

  @Test
  fun `test course`() {
    val course = course(courseMode = CourseMode.EDUCATOR, description = "This is a course about string theory.\nWhy not?") {
      lesson("the first lesson")
      lesson("the second lesson")
    }
    course.languageCode = "ru"
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
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
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
    """.trimMargin())
  }

  @Test
  fun `test lesson with custom presentable name`() {
    val lesson = course {
      lesson(customPresentableName = "my new lesson") {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    doTest(lesson, """
      |custom_name: ${lesson.customPresentableName}
      |content:
      |- Introduction Task
      |- Advanced Task
      |
    """.trimMargin())
  }

  @Test
  fun `test lesson with content tags`() {
    val lesson = course {
      lesson {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    lesson.contentTags = listOf("kotlin", "cycles")
    @Suppress("DEPRECATION") // using `customPresentableName` here is ok
    doTest(lesson, """
      |content:
      |- Introduction Task
      |- Advanced Task
      |tags:
      |- kotlin
      |- cycles
      |
    """.trimMargin())
  }

  @Test
  fun `test use dir name for lesson with custom name`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson(customPresentableName = "my new lesson") {
          eduTask("Introduction Task")
          eduTask("Advanced Task")
        }
      }
    }
    val section = course.sections.first()
    val lesson = section.lessons.first()

    doTest(section, """
      |content:
      |- ${lesson.name}
      |
    """.trimMargin())
  }

  @Test
  fun `test framework lesson`() {
    val lesson = course(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test framework lesson with content tags`() {
    val lesson = course(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    lesson.contentTags = listOf("kotlin", "cycles")
    doTest(lesson, """
      |type: framework
      |content:
      |- Introduction Task
      |- Advanced Task
      |tags:
      |- kotlin
      |- cycles
      |
    """.trimMargin())
  }

  @Test
  fun `test non templated based framework lesson`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("lesson", isTemplateBased = false)
    }

    doTest(course.getItem("lesson")!!, """
      |type: framework
      |is_template_based: false
      |
    """.trimMargin())
  }

  @Test
  fun `test framework lesson with custom name`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("lesson", customPresentableName = "my new lesson")
    }

    val lesson = course.getItem("lesson")!!

    doTest(lesson, """
      |type: framework
      |custom_name: my new lesson
      |
    """.trimMargin())
  }

  @Test
  fun `test section`() {
    val section = course(courseMode = CourseMode.EDUCATOR) {
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

  @Test
  fun `test section with custom name`() {
    val section = course(courseMode = CourseMode.EDUCATOR) {
      section(customPresentableName = "custom section name") {
        lesson("Introduction Lesson")
        lesson("Advanced Lesson")
      }
    }.items[0]

    doTest(section, """
      |custom_name: custom section name
      |content:
      |- Introduction Lesson
      |- Advanced Lesson
      |
    """.trimMargin())
  }

  @Test
  fun `test section with content tags`() {
    val section = course(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson("Introduction Lesson")
        lesson("Advanced Lesson")
      }
    }.items[0]

    section.contentTags = listOf("kotlin", "cycles")
    doTest(section, """
      |content:
      |- Introduction Lesson
      |- Advanced Lesson
      |tags:
      |- kotlin
      |- cycles
      |
    """.trimMargin())
  }

  @Test
  fun `test task feedback link`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
      }
    }.findTask("lesson1", "task1")
    task.feedbackLink = "example.com"
    doTest(task, """
    |type: edu
    |feedback_link: example.com
    |""".trimMargin())
  }

  @Test
  fun `test task with custom presentable name`() {
    val taskCustomName = "task custom name"
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask(customPresentableName = taskCustomName) { }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
    |type: edu
    |custom_name: $taskCustomName
    |""".trimMargin())
  }

  @Test
  fun `test course with environment`() {
    val course = course(courseMode = CourseMode.EDUCATOR, environment = EduNames.ANDROID) {
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
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test course feedback link`() {
    val courseLink = "https://course_link.com"
    val course = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
      }
    }.apply { feedbackLink = courseLink }
    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |feedback_link: $courseLink
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test hyperskill course`() {
    val course = course(courseProducer = ::HyperskillCourse) {} as HyperskillCourse
    course.apply {
      languageCode = "en"
    }

    doTest(course, """
      |type: hyperskill
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |mode: Study
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test stepik course`() {
    val course = course(courseProducer = ::StepikCourse) {} as StepikCourse
    course.apply {
      languageCode = "en"
    }

    doTest(course, """
      |type: stepik
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |mode: Study
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test coursera course feedback link not serialized`() {
    val courseLink = "https://course_link.com"
    val course = course(courseProducer = ::CourseraCourse) {
      lesson {
        eduTask { }
      }
    }.apply { feedbackLink = courseLink }
    doTest(course, """
      |type: coursera
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |mode: Study
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test course with hidden solutions`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {}
    course.solutionsHidden = true
    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |solutions_hidden: true
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test task with hidden solution`() {
    val taskCustomName = "task custom name"
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask(customPresentableName = taskCustomName) {}
      }
    }.findTask("lesson1", "task1")

    task.solutionHidden = true
    doTest(task, """
    |type: edu
    |custom_name: $taskCustomName
    |solution_hidden: true
    |""".trimMargin("|"))
  }

  @Test
  fun `test task with hidden solution = false`() {
    val taskCustomName = "task custom name"
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask(customPresentableName = taskCustomName) {}
      }
    }.findTask("lesson1", "task1")

    task.solutionHidden = false
    doTest(task, """
    |type: edu
    |custom_name: $taskCustomName
    |solution_hidden: false
    |""".trimMargin("|"))
  }

  @Test
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

  @Test
  fun `test empty section`() {
    val section = course(courseMode = CourseMode.EDUCATOR) {
      section {
      }
    }.items.first()

    doTest(section, """
      |{}
      |
    """.trimMargin())
  }

  @Test
  fun `test empty course`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {}

    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test course with lang version`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {}
    course.languageCode = "ru"
    course.description = "sum"
    course.languageId = PlainTextLanguage.INSTANCE.id
    course.languageVersion = "1.42"
    doTest(course, """
      |title: Test Course
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test course with content tags`() {
    val course = course(courseMode = CourseMode.EDUCATOR) {}
    course.contentTags = listOf("kotlin", "cycles")
    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |tags:
      |- kotlin
      |- cycles
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test coursera course`() {
    val course = course(courseMode = CourseMode.EDUCATOR, courseProducer = ::CourseraCourse) {}
    course.languageCode = "ru"
    course.description = "sum"
    course.languageId = PlainTextLanguage.INSTANCE.id
    course.languageVersion = "1.42"
    doTest(course, """
      |type: coursera
      |title: Test Course
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test coursera course manual submit`() {
    val course = course(courseMode = CourseMode.EDUCATOR, courseProducer = ::CourseraCourse) {} as CourseraCourse
    course.languageCode = "ru"
    course.description = "sum"
    course.submitManually = true
    course.languageId = PlainTextLanguage.INSTANCE.id
    course.languageVersion = "1.42"
    doTest(course, """
      |type: coursera
      |title: Test Course
      |language: Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |submit_manually: true
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test course with non-english locale`() {
    val defaultLocale = Locale.getDefault()
    Locale.setDefault(Locale.KOREAN)

    val course = course(courseMode = CourseMode.EDUCATOR, description = "This is a course about string theory.\nWhy not?") {
      lesson("the first lesson")
      lesson("the second lesson")
    }
    course.languageCode = "ru"
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
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())

    Locale.setDefault(defaultLocale)
  }

  @Test
  fun `test course with generatedEduId`() {
    val generatedEduId = "generated_edu_id"

    val course = course(courseMode = CourseMode.EDUCATOR, description = "This is a course about string theory.\nWhy not?") {
      lesson("the first lesson")
      lesson("the second lesson")
    } as EduCourse
    course.languageCode = "ru"
    course.generatedEduId = generatedEduId

    val actual = YamlMapper.remoteMapper().writeValueAsString(course)
    val expected = """
      |id: 0
      |generated_edu_id: generated_edu_id
      |
    """.trimMargin()
    assertEquals(expected, actual)
  }

  @Test
  fun `test course with Vendor`() {
    val vendor = Vendor().apply {
      name = "Vendor name"
      email = "email@gmail.com"
      url = "https://vendor.com/"
    }

    val course = course(courseMode = CourseMode.EDUCATOR, description = "This is a course about string theory.\nWhy not?") {
      lesson("the first lesson")
      lesson("the second lesson")
    }
    course.languageCode = "ru"
    course.vendor = vendor
    doTest(course, """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?
      |vendor:
      |  name: Vendor name
      |  email: email@gmail.com
      |  url: https://vendor.com/
      |programming_language: Plain text
      |content:
      |- the first lesson
      |- the second lesson
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin())
  }

  @Test
  fun `test course with choice tasks`() {
    val task = course(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        choiceTask(
          name = "task1",
          isMultipleChoice = true,
          choiceOptions = mapOf("1" to ChoiceOptionStatus.CORRECT, "2" to ChoiceOptionStatus.INCORRECT),
          messageCorrect = "You are genius!",
          messageIncorrect = "Try more",
          quizHeader = "Let's do it!",
        ) {
          taskFile("task.txt")
        }
      }
    }.findTask("lesson1", "task1")

    doTest(task, """
      |type: choice
      |is_multiple_choice: true
      |options:
      |- text: 1
      |  is_correct: true
      |- text: 2
      |  is_correct: false
      |message_correct: You are genius!
      |message_incorrect: Try more
      |quiz_header: Let's do it!
      |files:
      |- name: task.txt
      |  visible: true
      |local_check: true
      |""".trimMargin())
  }

  @Test
  fun `test environment settings`() {
    val course = course {
      lesson("lesson1") {
        eduTask()
      }
    }
    course.environmentSettings += "foo" to "bar"
    doTest(course, """
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |mode: Study
      |environment_settings:
      |  foo: bar
      |yaml_version: $CURRENT_YAML_VERSION
      |""".trimMargin())
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = item.course.mapper().writeValueAsString(item)
    assertEquals(expected, actual)
  }
}