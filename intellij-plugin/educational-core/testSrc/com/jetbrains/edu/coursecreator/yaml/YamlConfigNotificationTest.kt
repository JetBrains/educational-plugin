package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.NotificationsTestBase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.document
import com.jetbrains.edu.learning.yaml.YamlConfigNotificationProvider
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.YamlLoader
import org.junit.Test

class YamlConfigNotificationTest : NotificationsTestBase() {

  @Test
  fun `test correct config`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, createYamlConfigs = true) {
      lesson {
        eduTask("task1")
      }
    }

    val configFile = findFile("lesson1/task1/${YamlConfigSettings.TASK_CONFIG}")
    checkNoEditorNotification<YamlConfigNotificationProvider>(configFile)
  }

  @Test
  fun `test invalid config`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, createYamlConfigs = true) {
      lesson {
        eduTask("task1")
      }
    }

    val configFile = changeConfigFileAndLoad("lesson1/task1/${YamlConfigSettings.TASK_CONFIG}") {
      it.setText("random text")
    }

    checkEditorNotification<YamlConfigNotificationProvider>(
      configFile,
      "Failed to apply configuration: task type is not specified"
    )
  }

  @Test
  fun `test several invalid configs`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, createYamlConfigs = true) {
      lesson {
        eduTask("task1")
        eduTask("task2")
      }
    }

    val configFile1 = changeConfigFileAndLoad("lesson1/task1/${YamlConfigSettings.TASK_CONFIG}") {
      it.setText("random text")
    }
    val configFile2 = changeConfigFileAndLoad("lesson1/task2/${YamlConfigSettings.TASK_CONFIG}") {
      it.insertString(it.textLength, "\n0")
    }

    checkEditorNotification<YamlConfigNotificationProvider>(
      configFile1,
      "Failed to apply configuration: task type is not specified"
    )

    checkEditorNotification<YamlConfigNotificationProvider>(
      configFile2,
      "Failed to apply configuration: could not find expected ':' at line 3"
    )
  }

  @Test
  fun `test do not show yaml loading error in non config file`() {
    courseWithFiles(courseMode = CourseMode.EDUCATOR, createYamlConfigs = true) {
      lesson {
        eduTask("task1") {
          taskFile("TaskFile.txt")
        }
      }
    }

    changeConfigFileAndLoad("lesson1/task1/${YamlConfigSettings.TASK_CONFIG}") {
      it.setText("random text")
    }

    val taskFile = findFile("lesson1/task1/TaskFile.txt")
    checkNoEditorNotification<YamlConfigNotificationProvider>(taskFile)
  }

  private fun changeConfigFileAndLoad(configPath: String, applyChange: (Document) -> Unit): VirtualFile {
    val configFile = findFile(configPath)
    myFixture.openFileInEditor(configFile)
    WriteCommandAction.runWriteCommandAction(project) {
      applyChange(configFile.document)
    }
    withOriginalException {
      YamlLoader.loadItem(project, configFile, false)
    }
    return configFile
  }

  private fun withOriginalException(action: () -> Unit) {
    val previousValue = project.getUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION)
    try {
      project.putUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION, false)
      action()
    }
    finally {
      project.putUserData(YamlFormatSettings.YAML_TEST_THROW_EXCEPTION, previousValue)
    }
  }
}
