package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import java.util.*

class YamlRemoteSerializationTest : EduTestCase() {

  fun `test course`() {
    val course = course {
      lesson()
    } as EduCourse

    course.id = 1
    course.sectionIds = listOf(1)
    course.updateDate = Date(0)
    doTest(course, """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |default_section: 1
    |""".trimMargin("|"))
  }

  fun `test course without top-level lessons`() {
    val course = course {
      section()
    } as EduCourse

    course.id = 1
    course.updateDate = Date(0)
    doTest(course, """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin("|"))
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = YamlFormatSynchronizer.REMOTE_MAPPER.writeValueAsString(item)
    assertEquals(expected, actual)
  }
}