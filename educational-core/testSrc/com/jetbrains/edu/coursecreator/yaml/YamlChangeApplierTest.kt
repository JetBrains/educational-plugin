package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.coursera.CourseraCourse
import com.jetbrains.edu.learning.coursera.CourseraNames


class YamlChangeApplierTest : YamlTestCase() {
  override fun setUp() {
    super.setUp()
    project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, false)
  }

  fun `test coursera submit manually`() {
    val course = courseWithFiles(courseProducer = ::CourseraCourse, courseMode = CCUtils.COURSE_MODE) {
      lesson { }
    } as CourseraCourse
    assertFalse(course.submitManually)

    val yamlContent = """
      |type: ${CourseraNames.COURSE_TYPE}
      |submit_manually: true
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |content:
      |- lesson1
      |""".trimMargin("|")

    loadItemFromConfig(course, yamlContent)
    assertTrue(course.submitManually)
  }

  fun `test add lesson custom presentable name`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson { }
    }

    val lessonCustomName = "my best lesson"
    val yamlContent = """
      |custom_name: $lessonCustomName
    """.trimMargin("|")

    val lesson = findLesson(0)
    loadItemFromConfig(lesson, yamlContent)
    assertEquals(lessonCustomName, lesson.customPresentableName)
  }

  fun `test remove lesson custom presentable name`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask("Introduction Task")
      }
    }

    val lessonCustomName = "my best lesson"
    val lesson = findLesson(0)
    lesson.customPresentableName = lessonCustomName

    val yamlContent = """
      |content:
      |- Introduction Task
    """.trimMargin("|")

    loadItemFromConfig(lesson, yamlContent)
    assertNull(lesson.customPresentableName)
  }

  fun `test add section custom presentable name`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section { }
    }

    val customName = "my best lesson"
    val yamlContent = """
      |custom_name: $customName
    """.trimMargin("|")

    val section = course.sections[0]
    loadItemFromConfig(section, yamlContent)
    assertEquals(customName, section.customPresentableName)
  }

  fun `test remove section custom presentable name`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section {
        lesson("lesson 1")
      }
    }

    val customName = "my best lesson"
    val section = course.sections[0]
    section.customPresentableName = customName

    val yamlContent = """
      |content:
      |- lesson 1
    """.trimMargin("|")

    loadItemFromConfig(section, yamlContent)
    assertNull(section.customPresentableName)
  }

  fun `test add task custom presentable name`() {
    val task = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson 1") {
        eduTask {
          taskFile("task.txt")
        }
      }
    }.lessons[0].taskList[0]

    val customName = "task custom name"
    val yamlContent = """
      |type: edu
      |custom_name: $customName
      |files:
      |- name: task.txt
    """.trimMargin("|")

    loadItemFromConfig(task, yamlContent)
    assertEquals(customName, task.customPresentableName)
  }

  fun `test remove task custom presentable name`() {
    val task = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson("lesson 1") {
        eduTask {
          taskFile("task.txt")
        }
      }
    }.lessons[0].taskList[0]


    val customName = "my custom name"
    task.customPresentableName = customName

    val yamlContent = """
      |type: edu
      |files:
      |- name: task.txt
    """.trimMargin("|")

    loadItemFromConfig(task, yamlContent)
    assertNull(task.customPresentableName)
  }

  fun `test hide solutions from the learner`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson { }
    }
    assertFalse(course.solutionsHidden)

    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: My awesome summary
      |programming_language: Plain text
      |solutions_hidden: true
    """.trimMargin("|")

    loadItemFromConfig(course, yamlContent)
    assertTrue(course.solutionsHidden)
  }
}