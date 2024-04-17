package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.yaml.YamlFormatSettings
import com.jetbrains.edu.learning.yaml.checkConfigsExistAndNotEmpty
import org.junit.Test

class YamlGeneratorTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings = EmptyProjectSettings

  override fun setUp() {
    super.setUp()

    // copy-pasted from `YamlTestCase`
    runWriteAction { FileTypeManager.getInstance().associateExtension(PlainTextFileType.INSTANCE, "yaml") }
  }

  @Test
  fun `test create yaml files for new project`() {
    // project is created inside `createCourseStructure`, where also config files created
    // so we have to set this flag after project is created, but before config files creation
    val connection = ApplicationManager.getApplication().messageBus.connect()
    connection.subscribe(ProjectManager.TOPIC, object : ProjectManagerListener {
      override fun projectOpened(project: Project) {
        project.putUserData(YamlFormatSettings.YAML_TEST_PROJECT_READY, true)
        connection.disconnect()
      }
    })

    val course = course(courseMode = CourseMode.EDUCATOR) {
      lesson("lesson1") {
        eduTask("task1")
      }
      section {
        lesson {
          eduTask()
        }
      }
    }
    createCourseStructure(course)
    UIUtil.dispatchAllInvocationEvents()

    checkConfigsExistAndNotEmpty(project, course)
  }
}