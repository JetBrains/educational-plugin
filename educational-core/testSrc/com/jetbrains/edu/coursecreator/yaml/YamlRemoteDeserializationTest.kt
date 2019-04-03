package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.coursecreator.yaml.YamlFormatSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.courseFormat.EduCourse
import java.util.*

class YamlRemoteDeserializationTest : EduTestCase() {

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
}