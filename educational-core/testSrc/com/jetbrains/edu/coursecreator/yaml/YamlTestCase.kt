package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.testFramework.UsefulTestCase
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.YAML_TEST_PROJECT_READY
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.YAML_TEST_THROW_EXCEPTION
import com.jetbrains.edu.learning.yaml.YamlLoader
import com.jetbrains.edu.learning.yaml.configFileName

abstract class YamlTestCase : EduTestCase() {
  override fun setUp() {
    super.setUp()

    // In this method course is set before course files are created so `CCProjectComponent.createYamlConfigFilesIfMissing` is called
    // for course with no files. This flag is checked in this method and it does nothing if the flag is false
    project.putUserData(YAML_TEST_PROJECT_READY, true)
    project.putUserData(YAML_TEST_THROW_EXCEPTION, true)

    // we need to specify the file type for yaml files as otherwise they are recognised as binaries and aren't allowed to be edited
    // we don't add dependency on yaml plugin because it's impossible to add for tests only and we don't want to have redundant dependency
    // in production code
    runWriteAction { FileTypeManager.getInstance().associateExtension(PlainTextFileType.INSTANCE, "yaml") }
  }

  protected fun loadItemFromConfig(item: StudyItem, newConfigText: String) {
    createConfigFiles(project)
    val configFile = item.getDir(project)!!.findChild(item.configFileName)!!
    val document = FileDocumentManager.getInstance().getDocument(configFile)!!
    runWriteAction {
      document.setText(newConfigText)
    }

    UIUtil.dispatchAllInvocationEvents()
    YamlLoader.loadItem(project, configFile)
  }
}

fun checkConfigsExistAndNotEmpty(project: Project, course: Course) {
  course.items.forEach { courseItem ->
    checkConfig(project, courseItem)

    // checking sections/top-level lessons
    (courseItem as ItemContainer).items.forEach {
      checkConfig(project, it)
      if (it is Lesson) {
        it.items.forEach { task -> checkConfig(project, task) }
      }
    }
  }
}

private fun checkConfig(project: Project, item: StudyItem) {
  val itemDir = item.getDir(project)
  val configFileName = item.configFileName
  val configFile = itemDir.findChild(configFileName)!!
  val configText = FileDocumentManager.getInstance().getDocument(configFile)!!.text
  UsefulTestCase.assertNotNull("Config file shouldn't be null", configFile)
  UsefulTestCase.assertTrue("Config file should not be empty: ${configFile.name}", configText.isNotEmpty())
}

