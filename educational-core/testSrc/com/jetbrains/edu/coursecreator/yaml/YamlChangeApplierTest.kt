package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.CourseraCourse
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.findTask
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlTestCase
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames


class YamlChangeApplierTest : YamlTestCase() {
  override fun setUp() {
    super.setUp()
    project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, false)
  }

  fun `test coursera submit manually`() {
    val course = courseWithFiles(courseProducer = ::CourseraCourse, courseMode = CourseMode.EDUCATOR) {
      lesson { }
    } as CourseraCourse
    assertFalse(course.submitManually)

    val yamlContent = """
      |type: ${YamlMixinNames.COURSE_TYPE_YAML}
      |submit_manually: true
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |content:
      |- lesson1
      |""".trimMargin()

    loadItemFromConfig(course, yamlContent)
    assertTrue(course.submitManually)
  }

  fun `test add lesson custom presentable name`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson { }
    }

    val lessonCustomName = "my best lesson"
    val yamlContent = """
      |custom_name: $lessonCustomName
    """.trimMargin()

    val lesson = findLesson(0)
    loadItemFromConfig(lesson, yamlContent)
    @Suppress("DEPRECATION")
    assertEquals(lessonCustomName, lesson.customPresentableName)
  }

  fun `test remove lesson custom presentable name`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson(customPresentableName = "my best lesson") {
        eduTask("Introduction Task")
      }
    }

    val lesson = findLesson(0)
    val yamlContent = """
      |content:
      |- Introduction Task
    """.trimMargin()

    loadItemFromConfig(lesson, yamlContent)
    @Suppress("DEPRECATION")
    assertNull(lesson.customPresentableName)
  }

  fun `test add section custom presentable name`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section { }
    }

    val customName = "my best lesson"
    val yamlContent = """
      |custom_name: $customName
    """.trimMargin()

    val section = course.sections[0]
    loadItemFromConfig(section, yamlContent)
    @Suppress("DEPRECATION")
    assertEquals(customName, section.customPresentableName)
  }

  fun `test remove section custom presentable name`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section(customPresentableName = "my best lesson") {
        lesson("lesson 1")
      }
    }

    val section = course.sections[0]
    val yamlContent = """
      |content:
      |- lesson 1
    """.trimMargin()

    loadItemFromConfig(section, yamlContent)
    @Suppress("DEPRECATION")
    assertNull(section.customPresentableName)
  }

  fun `test add task custom presentable name`() {
    val task = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
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
    """.trimMargin()

    loadItemFromConfig(task, yamlContent)
    @Suppress("DEPRECATION")
    assertEquals(customName, task.customPresentableName)
  }

  fun `test remove task custom presentable name`() {
    val task = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson 1") {
        eduTask(customPresentableName = "my custom name") {
          taskFile("task.txt")
        }
      }
    }.lessons[0].taskList[0]

    val yamlContent = """
      |type: edu
      |files:
      |- name: task.txt
    """.trimMargin()

    loadItemFromConfig(task, yamlContent)
    @Suppress("DEPRECATION")
    assertNull(task.customPresentableName)
  }

  fun `test add hide solutions for course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson { }
    }
    assertFalse(course.solutionsHidden)

    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: My awesome summary
      |programming_language: Plain text
      |solutions_hidden: true
    """.trimMargin()

    loadItemFromConfig(course, yamlContent)
    assertTrue(course.solutionsHidden)
  }

  fun `test remove hide solutions for course`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson { }
    }
    course.solutionsHidden = true

    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: My awesome summary
      |programming_language: Plain text
    """.trimMargin("|")

    loadItemFromConfig(course, yamlContent)
    assertFalse(course.solutionsHidden)
  }

  fun `test add hide solution for task`() {
    val task = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1")
      }
    }.findTask("lesson1", "task1")
    assertNull(task.solutionHidden)

    val yamlContent = """
      |type: edu
      |solution_hidden: false
    """.trimMargin("|")

    loadItemFromConfig(task, yamlContent)
    assertFalse(task.solutionHidden!!)
  }

  fun `test remove hide solution for task`() {
    val task = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1")
      }
    }.findTask("lesson1", "task1")
    task.solutionHidden = true

    val yamlContent = """
      |type: edu
    """.trimMargin("|")

    loadItemFromConfig(task, yamlContent)
    assertNull(task.solutionHidden)
  }

  fun `test change is_template_based flag in framework lesson`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      frameworkLesson("lesson1") {
        eduTask("task1")
      }
    }
    val lesson = course.lessons.single() as FrameworkLesson
    val yamlContent = """
      |type: framework
      |content:
      |- task1
      |is_template_based: false
    """.trimMargin("|")
    loadItemFromConfig(lesson, yamlContent)
    assertFalse(lesson.isTemplateBased)
  }
}
