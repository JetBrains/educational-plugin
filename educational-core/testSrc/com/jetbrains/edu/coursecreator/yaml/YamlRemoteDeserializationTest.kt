package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_LESSON_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.Lesson
import java.util.*

class YamlRemoteDeserializationTest : YamlTestCase() {

  fun `test course`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |default_section: $id
    |""".trimMargin("|")

    val course = YamlDeserializer.deserializeRemoteItem(yamlText, REMOTE_COURSE_CONFIG) as EduCourse
    assertEquals(1, course.id)
    assertEquals(Date(0), course.updateDate)
    assertEquals(listOf(1), course.sectionIds)
  }

  fun `test course without top-level lessons`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin("|")

    val course = YamlDeserializer.deserializeRemoteItem(yamlText, REMOTE_COURSE_CONFIG) as EduCourse
    assertEquals(1, course.id)
    assertEquals(Date(0), course.updateDate)
    assertTrue(course.sectionIds.isEmpty())
  }

  fun `test section`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin("|")

    val section = YamlDeserializer.deserializeRemoteItem(yamlText, REMOTE_SECTION_CONFIG)
    assertEquals(1, section.id)
    assertEquals(Date(0), section.updateDate)
  }

  fun `test top-level lesson`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |unit: $id
    |""".trimMargin("|")

    val lesson = YamlDeserializer.deserializeRemoteItem(yamlText, REMOTE_LESSON_CONFIG) as Lesson
    assertEquals(1, lesson.id)
    assertEquals(1, lesson.unitId)
    assertEquals(Date(0), lesson.updateDate)
  }

  fun `test task`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin("|")

    val task = YamlDeserializer.deserializeRemoteItem(yamlText, REMOTE_TASK_CONFIG)
    assertEquals(1, task.id)
    assertEquals(Date(0), task.updateDate)
  }
}