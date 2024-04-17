package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlTestCase
import org.junit.Test


class YamlRemoveItemTest : YamlTestCase() {
  override fun setUp() {
    super.setUp()
    project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, false)
  }

  @Test
  fun `test remove lesson`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson { }
      lesson { }
    }

    loadItemFromConfig(getCourse(), """
      |title: Test Course
      |language: English
      |programming_language: Plain text
      |summary: text
      |content:
      |- lesson1
      |
    """.trimMargin())

    assertEquals(1, getCourse().items.size)
    assertEquals("lesson1", findLesson(0).name)
  }

  @Test
  fun `test remove section`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section {
        lesson("lesson1") { }
      }
      section {
        lesson("lesson2") { }
      }
    }

    loadItemFromConfig(getCourse(), """
      |title: Test Course
      |language: English
      |programming_language: Plain text
      |summary: text
      |content:
      |- section2
      |
    """.trimMargin())

    assertEquals(1, getCourse().items.size)
    assertEquals("section2", getCourse().items[0].name)
  }

  @Test
  fun `test remove task`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
        eduTask { }
        eduTask { }
      }
    }

    val lesson = findLesson(0)
    loadItemFromConfig(lesson, """
      |content:
      |- task1
      |- task3
      |
    """.trimMargin())

    assertEquals(2, lesson.taskList.size)
    assertEquals("task1", lesson.taskList[0].name)
    assertEquals("task3", lesson.taskList[1].name)
  }

  @Test
  fun `test task file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("task1.txt")
          taskFile("task2.txt")
        }
      }
    }

    val task = findTask(0, 0)
    loadItemFromConfig(task, """
        |type: edu
        |files:
        |- name: task1.txt
        |  visible: true
    """.trimMargin())

    assertEquals(1, task.taskFiles.size)
    assertEquals("task1.txt", task.taskFiles.keys.single())
  }
}