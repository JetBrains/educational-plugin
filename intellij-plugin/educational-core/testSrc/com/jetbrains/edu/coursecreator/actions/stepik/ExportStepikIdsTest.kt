package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikLesson
import org.intellij.lang.annotations.Language
import org.junit.Test

class ExportStepikIdsTest : EduTestCase() {

  @Test
  fun `test not available for local course`() {
    courseWithFiles {
      lesson { eduTask { } }
      section { lesson { eduTask { } } }
    }
    testAction(ExportStepikIds.ACTION_ID, shouldBeEnabled = false)
  }

  @Test
  fun `test export stepik ids`() {
    val course = courseWithFiles {
      stepikLesson { eduTask { } }
      section { stepikLesson { eduTask { } } }
    }

    val remoteCourse = convertToRemoteCourse(course)

    remoteCourse.generateUniqueIds()

    StudyTaskManager.getInstance(project).course = remoteCourse

    testAction(ExportStepikIds.ACTION_ID)
    val courseDir = project.courseDir
    val stepikIdsFile = courseDir.findChild(EduNames.STEPIK_IDS_JSON) ?: error("file with wasn't created")
    val actualFileContent = VfsUtil.loadText(stepikIdsFile)

    @Language("JSON")
    val expectedFileContent = """
{
  "title": "Test Course",
  "id": 1,
  "items": [
    {
      "title": "lesson1",
      "id": 11,
      "task_list": [
        {
          "title": "task1",
          "id": 111
        }
      ],
      "unit_id": 11
    },
    {
      "title": "section2",
      "id": 2,
      "items": [
        {
          "title": "lesson1",
          "id": 21,
          "task_list": [
            {
              "title": "task1",
              "id": 211
            }
          ],
          "unit_id": 21
        }
      ]
    }
  ],
  "sectionIds": [
    1
  ]
}
    """.trimIndent()

    assertEquals(expectedFileContent, actualFileContent)
  }

  private fun convertToRemoteCourse(course: Course): EduCourse {
    val remoteCourse = StepikCourse()
    remoteCourse.name = course.name
    remoteCourse.courseMode = CourseMode.EDUCATOR
    remoteCourse.items = course.items
    return remoteCourse
  }

  private fun EduCourse.generateUniqueIds() {
    id = 1
    sections[0].id = 2
    val newSectionIds = mutableListOf<Int>()
    visitLessons { lesson ->
      val section = lesson.section
      val sectionId = section?.id ?: 1
      if (section == null) {
        newSectionIds.add(lesson.index)
      }
      lesson.id = 10 * sectionId + lesson.index
      if (lesson is StepikLesson) {
        lesson.unitId = lesson.id
      }
      for (task in lesson.taskList) {
        task.id = 10 * lesson.id + task.index
      }
    }
    sectionIds = newSectionIds
  }
}
