package com.jetbrains.edu.coursecreator.yaml

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.yaml.GeneratedRemoteInfoNotificationProvider
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import org.junit.Test

class RemoteInfoNotificationTest : NotificationsTestBase() {

  @Test
  fun `test course remote notification`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {}
    val yamlText = createRemoteYamlConfigText()

    withYamlFileTypeRegistered {
      val configFile = GeneratorUtils.createTextChildFile(project, LightPlatformTestCase.getSourceRoot(), YamlConfigSettings.REMOTE_COURSE_CONFIG, yamlText)
      checkEditorNotification<GeneratedRemoteInfoNotificationProvider>(configFile!!)
    }
  }

  @Test
  fun `test section remote notification`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      section { }
    }
    val yamlText = createRemoteYamlConfigText()

    val sectionDir = LightPlatformTestCase.getSourceRoot().findChild(course.sections[0].name)!!
    withYamlFileTypeRegistered {
      val configFile = GeneratorUtils.createTextChildFile(project, sectionDir, YamlConfigSettings.REMOTE_SECTION_CONFIG, yamlText)
      checkEditorNotification<GeneratedRemoteInfoNotificationProvider>(configFile!!)
    }
  }

  @Test
  fun `test lesson remote notification`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson { }
    }
    val yamlText = createRemoteYamlConfigText()

    val lessonDir = course.lessons[0].getDir(project.courseDir)!!
    withYamlFileTypeRegistered {
      val configFile = GeneratorUtils.createTextChildFile(project, lessonDir, YamlConfigSettings.REMOTE_LESSON_CONFIG, yamlText)
      checkEditorNotification<GeneratedRemoteInfoNotificationProvider>(configFile!!)
    }
  }

  @Test
  fun `test task remote notification`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
      }
    }
    val yamlText = createRemoteYamlConfigText()

    val taskDir = course.lessons[0].taskList[0].getDir(project.courseDir)!!
    withYamlFileTypeRegistered {
      val configFile = GeneratorUtils.createTextChildFile(project, taskDir, YamlConfigSettings.REMOTE_TASK_CONFIG, yamlText)
      checkEditorNotification<GeneratedRemoteInfoNotificationProvider>(configFile!!)
    }
  }

  @Test
  fun `test non config file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask {
          taskFile("task.txt", "empty text")
        }
      }
    }

    val virtualFile = findFileInTask(0, 0, "task.txt")
    myFixture.openFileInEditor(virtualFile)
    checkNoEditorNotification<GeneratedRemoteInfoNotificationProvider>(virtualFile)
  }

  @Test
  fun `test local config file`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
      }
    }
    val yamlText = createRemoteYamlConfigText()

    val taskDir = course.lessons[0].taskList[0].getDir(project.courseDir)!!
    withYamlFileTypeRegistered {
      val configFile = GeneratorUtils.createTextChildFile(project, taskDir, YamlConfigSettings.TASK_CONFIG, yamlText)
      checkNoEditorNotification<GeneratedRemoteInfoNotificationProvider>(configFile!!)
    }
  }

  private fun createRemoteYamlConfigText(): String {
    return """
      |id: 2
      |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    """.trimIndent()
  }
}