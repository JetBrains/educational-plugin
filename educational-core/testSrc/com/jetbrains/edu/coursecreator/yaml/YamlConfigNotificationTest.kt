package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runWriteAction
import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.yaml.YamlConfigNotificationProvider
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlLoader

class YamlConfigNotificationTest : NotificationsTestBase() {

  fun `test correct config`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
      }
    }

    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getDir(project.courseDir)
    createConfigFiles(project)
    val configFile = taskDir!!.findChild(YamlFormatSettings.TASK_CONFIG)
    checkNoEditorNotification<YamlConfigNotificationProvider>(configFile!!)
  }

  fun `test invalid config`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR) {
      lesson {
        eduTask { }
      }
    }

    val task = course.findTask("lesson1", "task1")
    val taskDir = task.getDir(project.courseDir)
    createConfigFiles(project)
    val configFile = taskDir!!.findChild(YamlFormatSettings.TASK_CONFIG)!!
    myFixture.openFileInEditor(configFile)
    runWriteAction { configFile.document.setText("random text") }
    withOriginalException {
      YamlLoader.loadItem(project, configFile, false)
    }
    checkEditorNotification<YamlConfigNotificationProvider>(configFile,
                            "Failed to apply configuration: task type is not specified")
  }

  private fun withOriginalException(action: () -> Unit) {
    val previousValue = project.getUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION)
    try {
      project.putUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION, false)
      action()
    } finally {
      project.putUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION, previousValue)
    }
  }
}