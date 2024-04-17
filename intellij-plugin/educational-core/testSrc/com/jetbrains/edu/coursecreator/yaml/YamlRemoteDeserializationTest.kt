package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.attempts.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
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
    |default_section: $id
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as EduCourse
    assertEquals(1, course.id)
    assertEquals(Date(0), course.updateDate)
    assertEquals(listOf(1), course.sectionIds)
  }

  @Test
  fun `test marketplace course`() {
    val id = 1
    val yamlText = """
    |id: $id
    |course_version: 5
    |default_section: $id
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as EduCourse
    assertEquals(1, course.id)
    assertEquals(5, course.marketplaceCourseVersion)
  }

  @Test
  fun `test hyperskill project`() {
    val id = 15
    val ideFiles = "ideFiles"
    val isTemplateBased = true
    val yamlContent = """
      |hyperskill_project:
      |  id: $id
      |  ide_files: $ideFiles
      |  is_template_based: $isTemplateBased
      |update_date: Thu, 01 Jan 1970 00:00:00 UTC
      |stages:
      |- id: 1
      |  step: 11
      |  is_completed: true
      |- id: 2
      |  step: 22
      |  is_completed: false
      |topics:
      |  0:
      |  - title: Learn Anything
      |    theory_id: 404
      |
    """.trimMargin()

    val configFile = createConfigFile(yamlContent, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as HyperskillCourse

    val hyperskillProject = course.hyperskillProject!!
    assertEquals(id, hyperskillProject.id)
    assertEquals(ideFiles, hyperskillProject.ideFiles)
    assertEquals(isTemplateBased, hyperskillProject.isTemplateBased)

    assertEquals(Date(0), course.updateDate)

    checkStage(HyperskillStage(1, "", 11, true), course.stages[0])
    checkStage(HyperskillStage(2, "", 22, false), course.stages[1])

    val hyperskillTopic = course.taskToTopics[0]!!.first()
    assertEquals(404, hyperskillTopic.theoryId)
    assertEquals("Learn Anything", hyperskillTopic.title)
  }

  private fun checkStage(expected: HyperskillStage, actual: HyperskillStage) {
    assertEquals(expected.id, actual.id)
    assertEquals(expected.stepId, actual.stepId)
    assertEquals(expected.isCompleted, actual.isCompleted)
  }

  @Test
  fun `test course without top-level lessons`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as EduCourse
    assertEquals(1, course.id)
    assertEquals(Date(0), course.updateDate)
    assertTrue(course.sectionIds.isEmpty())
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
  fun `test data task without attempt`() {
    val yamlText = """
    |type: dataset
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_TASK_CONFIG)
    val task = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as DataTask
    assertEquals(1, task.id)
    assertEquals(Date(0), task.updateDate)
  }

  @Test
  fun `test data task with attempt`() {
    val yamlText = """
    |type: dataset
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |attempt:
    |  id: 2
    |  dataset_file: data/dataset1/input.txt
    |  end_date_time: Thu, 01 Jan 1970 00:05:00 UTC
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_TASK_CONFIG)
    val task = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as DataTask
    assertEquals(1, task.id)
    assertEquals(Date(0), task.updateDate)
    val attempt = Attempt(2, Date(0), 300).toDataTaskAttempt()
    assertEquals(attempt, task.attempt)
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