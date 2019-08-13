package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import java.util.*

class YamlRemoteSerializationTest : YamlTestCase() {

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

  fun `test section`() {
    val section = course {
      section()
    }.sections.first()

    section.id = 1
    section.updateDate = Date(0)
    doTest(section, """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin("|"))
  }

  fun `test lesson`() {
    val lesson = course {
      lesson()
    }.lessons.first()

    lesson.id = 1
    lesson.updateDate = Date(0)
    lesson.unitId = 1
    doTest(lesson, """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |unit: 1
    |""".trimMargin("|"))
  }

  fun `test task`() {
    val task = course {
      lesson {
        eduTask()
      }
    }.lessons.first().taskList.first()

    task.id = 1
    task.updateDate = Date(0)
    doTest(task, """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin("|"))
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = YamlFormatSynchronizer.REMOTE_MAPPER.writeValueAsString(item)
    assertEquals(expected, actual)
  }
}