package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.attempts.Attempt
import com.jetbrains.edu.learning.courseFormat.attempts.DataTaskAttempt.Companion.toDataTaskAttempt
import com.jetbrains.edu.learning.courseFormat.stepik.StepikLesson
import com.jetbrains.edu.learning.courseFormat.tasks.DataTask
import com.jetbrains.edu.learning.yaml.YamlMapper
import com.jetbrains.edu.learning.yaml.YamlTestCase
import org.junit.Test
import java.util.*

class YamlRemoteSerializationTest : YamlTestCase() {

  @Test
  fun `test course`() {
    val course = course {
      lesson()
    } as EduCourse

    course.id = 1
    course.sectionIds = listOf(1)
    course.updateDate = Date("Fri, 01 Jan 2010 00:00:00 UTC")
    doTest(
      course, """
    |id: 1
    |update_date: "Fri, 01 Jan 2010 00:00:00 UTC"
    |default_section: 1
    |""".trimMargin()
    )
  }

  @Test
  fun `test course without top-level lessons`() {
    val course = course {
      section()
    } as EduCourse

    course.id = 1
    course.updateDate = Date("Fri, 01 Jan 2010 00:00:00 UTC")
    doTest(
      course, """
    |id: 1
    |update_date: "Fri, 01 Jan 2010 00:00:00 UTC"
    |""".trimMargin()
    )
  }

  @Test
  fun `test section`() {
    val section = course {
      section()
    }.sections.first()

    section.id = 1
    section.updateDate = Date("Fri, 01 Jan 2010 00:00:00 UTC")
    doTest(
      section, """
    |id: 1
    |update_date: "Fri, 01 Jan 2010 00:00:00 UTC"
    |""".trimMargin()
    )
  }

  @Test
  fun `test lesson`() {
    val lesson = course {
      stepikLesson()
    }.lessons.first() as StepikLesson

    lesson.id = 1
    lesson.updateDate = Date(0)
    lesson.unitId = 1
    doTest(
      lesson, """
    |id: 1
    |unit: 1
    |""".trimMargin()
    )
  }

  @Test
  fun `test lesson default unit`() {
    val lesson = course {
      stepikLesson()
    }.lessons.first() as StepikLesson

    lesson.id = 1
    lesson.updateDate = Date(0)
    doTest(
      lesson, """
    |id: 1
    |""".trimMargin()
    )
  }

  @Test
  fun `test task`() {
    val task = course {
      lesson {
        eduTask()
      }
    }.lessons.first().taskList.first()

    task.id = 1
    task.updateDate = Date("Fri, 01 Jan 2010 00:00:00 UTC")
    doTest(
      task, """
    |id: 1
    |update_date: "Fri, 01 Jan 2010 00:00:00 UTC"
    |""".trimMargin()
    )
  }

  @Test
  fun `test data task without attempt`() {
    val task = course {
      lesson {
        dataTask(stepId = 1, updateDate = Date("Fri, 01 Jan 2010 00:00:00 UTC"))
      }
    }.lessons.first().taskList.first() as DataTask

    doTest(
      task, """
    |type: dataset
    |id: 1
    |update_date: "Fri, 01 Jan 2010 00:00:00 UTC"
    |""".trimMargin()
    )
  }

  @Test
  fun `test data task with attempt`() {
    val task = course {
      lesson {
        dataTask(
          stepId = 1,
          updateDate = Date("Fri, 01 Jan 2010 00:00:00 UTC"),
          attempt = Attempt(2, Date(0), 300).toDataTaskAttempt()
        )
      }
    }.lessons.first().taskList.first() as DataTask

    doTest(
      task, """
    |type: dataset
    |id: 1
    |update_date: "Fri, 01 Jan 2010 00:00:00 UTC"
    |attempt:
    |  id: 2
    |  end_date_time: "Thu, 01 Jan 1970 00:05:00 UTC"
    |""".trimMargin()
    )
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = YamlMapper.remoteMapper().writeValueAsString(item)
    assertEquals(expected, actual)
  }
}