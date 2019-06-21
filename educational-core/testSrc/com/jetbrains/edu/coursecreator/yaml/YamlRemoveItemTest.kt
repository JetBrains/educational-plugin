package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSynchronizer.configFileName
import com.jetbrains.edu.learning.courseFormat.StudyItem


class YamlRemoveItemTest : YamlTestCase() {
  override fun setUp() {
    super.setUp()
    project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, false)
  }

  fun `test remove lesson`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
    """.trimMargin("|"))

    assertEquals(1, getCourse().items.size)
    assertEquals("lesson1", findLesson(0).name)
  }

  fun `test remove section`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
    """.trimMargin("|"))

    assertEquals(1, getCourse().items.size)
    assertEquals("section2", getCourse().items[0].name)
  }

  fun `test remove task`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
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
    """.trimMargin("|"))

    assertEquals(2, lesson.taskList.size)
    assertEquals("task1", lesson.taskList[0].name)
    assertEquals("task3", lesson.taskList[1].name)
  }

  fun `test task file`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("task1.txt")
          taskFile("task2.txt")
        }
      }
    }
    createConfigFiles()

    val task = findTask(0, 0)
    loadItemFromConfig(task, """
        |type: edu
        |files:
        |- name: task1.txt
        |  visible: true
    """.trimMargin("|"))

    assertEquals(1, task.taskFiles.size)
    assertEquals("task1.txt", task.taskFiles.keys.single())
  }

  private fun loadItemFromConfig(item: StudyItem, newConfigText: String) {
    createConfigFiles()
    val configFile = item.getDir(project)!!.findChild(item.configFileName)!!
    val document = FileDocumentManager.getInstance().getDocument(configFile)!!
    runWriteAction {
      document.setText(newConfigText)
    }
    YamlLoader.doLoad(project, configFile)
  }
}