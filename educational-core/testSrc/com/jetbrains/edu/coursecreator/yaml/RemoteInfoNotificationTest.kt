package com.jetbrains.edu.coursecreator.yaml

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.yaml.GeneratedRemoteInfoNotificationProvider
import com.jetbrains.edu.learning.yaml.YamlFormatSettings

class RemoteInfoNotificationTest : NotificationsTestBase() {

  fun `test course remote notification`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {}
    val yamlText = """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val configFile = GeneratorUtils.createChildFile(project, LightPlatformTestCase.getSourceRoot(), YamlFormatSettings.REMOTE_COURSE_CONFIG, yamlText)
    withYamlFileTypeRegistered {
      checkEditorNotification(configFile!!, GeneratedRemoteInfoNotificationProvider.KEY)
    }
  }

  fun `test section remote notification`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      section { }
    }
    val yamlText = """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val sectionDir = LightPlatformTestCase.getSourceRoot().findChild(course.sections[0].name)!!
    val configFile = GeneratorUtils.createChildFile(project, sectionDir, YamlFormatSettings.REMOTE_SECTION_CONFIG, yamlText)
    withYamlFileTypeRegistered {
      checkEditorNotification(configFile!!, GeneratedRemoteInfoNotificationProvider.KEY)
    }
  }

  fun `test lesson remote notification`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson { }
    }
    val yamlText = """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val lessonDir = course.lessons[0].getDir(project.courseDir)!!
    val configFile = GeneratorUtils.createChildFile(project, lessonDir, YamlFormatSettings.REMOTE_LESSON_CONFIG, yamlText)
    withYamlFileTypeRegistered {
      checkEditorNotification(configFile!!, GeneratedRemoteInfoNotificationProvider.KEY)
    }
  }

  fun `test task remote notification`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
      }
    }
    val yamlText = """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val taskDir = course.lessons[0].taskList[0].getDir(project.courseDir)!!
    val configFile = GeneratorUtils.createChildFile(project, taskDir, YamlFormatSettings.REMOTE_TASK_CONFIG, yamlText)
    withYamlFileTypeRegistered {
      checkEditorNotification(configFile!!, GeneratedRemoteInfoNotificationProvider.KEY)
    }
  }

  fun `test non config file`() {
    courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask {
          taskFile("task.txt")
        }
      }
    }

    val virtualFile = findFileInTask(0, 0, "task.txt")
    myFixture.openFileInEditor(virtualFile)
    checkNoEditorNotification(virtualFile, GeneratedRemoteInfoNotificationProvider.KEY)
  }

  fun `test local config file`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE) {
      lesson {
        eduTask { }
      }
    }
    val yamlText = """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val taskDir = course.lessons[0].taskList[0].getDir(project.courseDir)!!
    val configFile = GeneratorUtils.createChildFile(project, taskDir, YamlFormatSettings.TASK_CONFIG, yamlText)
    withYamlFileTypeRegistered {
      checkNoEditorNotification(configFile!!, GeneratedRemoteInfoNotificationProvider.KEY)
    }
  }

}