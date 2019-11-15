package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse
import com.jetbrains.edu.learning.yaml.YamlDeserializer
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.REMOTE_SECTION_CONFIG
import java.util.*

class YamlRemoteDeserializationTest : YamlTestCase() {

  fun `test course`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |default_section: $id
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile) as EduCourse
    assertEquals(1, course.id)
    assertEquals(Date(0), course.updateDate)
    assertEquals(listOf(1), course.sectionIds)
  }

  fun `test hyperskill project`() {
    val id = 15
    val ideFiles = "ideFiles"
    val isTemplateBased = true
    val yamlContent = """
      |hyperskill_project:
      |  id: $id
      |  ide_files: $ideFiles
      |  is_template_based: $isTemplateBased
      |
    """.trimMargin()

    val configFile = createConfigFile(yamlContent, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile) as HyperskillCourse

    val hyperskillProject = course.hyperskillProject
    assertEquals(id, hyperskillProject.id)
    assertEquals(ideFiles, hyperskillProject.ideFiles)
    assertEquals(isTemplateBased, hyperskillProject.isTemplateBased)
  }

  fun `test course without top-level lessons`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile) as EduCourse
    assertEquals(1, course.id)
    assertEquals(Date(0), course.updateDate)
    assertTrue(course.sectionIds.isEmpty())
  }

  fun `test section`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_SECTION_CONFIG)
    val section = YamlDeserializer.deserializeRemoteItem(configFile)
    assertEquals(1, section.id)
    assertEquals(Date(0), section.updateDate)
  }

  fun `test top-level lesson`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |unit: $id
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_LESSON_CONFIG)
    val lesson = YamlDeserializer.deserializeRemoteItem(configFile) as Lesson
    assertEquals(1, lesson.id)
    assertEquals(1, lesson.unitId)
    assertEquals(Date(0), lesson.updateDate)
  }

  fun `test task`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_LESSON_CONFIG)
    val task = YamlDeserializer.deserializeRemoteItem(configFile)
    assertEquals(1, task.id)
    assertEquals(Date(0), task.updateDate)
  }

  private fun createConfigFile(yamlText: String, configName: String): LightVirtualFile {
    val configFile = LightVirtualFile(configName)
    runWriteAction { VfsUtil.saveText(configFile, yamlText) }
    return configFile
  }
}