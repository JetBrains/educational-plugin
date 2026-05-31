package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlDeserializer
import com.jetbrains.edu.learning.yaml.YamlTestCase
import com.jetbrains.edu.learning.yaml.format.RemoteStudyItem
import org.junit.Test
import java.util.*

class YamlRemoteDeserializationTest : YamlTestCase() {

  @Test
  fun `test course`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as EduCourse
    assertEquals(1, course.id)
    assertEquals(Date(0), course.updateDate)
  }

  @Test
  fun `test marketplace course`() {
    val id = 1
    val yamlText = """
    |id: $id
    |course_version: 5
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as EduCourse
    assertEquals(1, course.id)
    assertEquals(5, course.marketplaceCourseVersion)
  }

  @Test
  fun `test section`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_SECTION_CONFIG)
    val section = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile))
    assertEquals(1, section.id)
    assertEquals(Date(0), section.updateDate)
  }

  @Test
  fun `test task`() {
    val yamlText = """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_TASK_CONFIG)
    val task = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as RemoteStudyItem
    assertEquals(1, task.id)
    assertEquals(Date(0), task.updateDate)
  }

  @Test
  fun `test quoted date`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: "Thu, 01 Jan 1970 00:00:01 UTC"
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as EduCourse
    assertEquals(1, course.id)
    assertEquals(Date(1000), course.updateDate)
  }

  private fun createConfigFile(yamlText: String, configName: String): LightVirtualFile {
    val configFile = LightVirtualFile(configName)
    runWriteAction { VfsUtil.saveText(configFile, yamlText) }
    return configFile
  }
}