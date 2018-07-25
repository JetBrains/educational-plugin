package com.jetbrains.edu.coursecreator.actions.stepik

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.vfs.VfsUtil
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.RemoteCourse
import org.intellij.lang.annotations.Language

class ExportStepikIdsTest : EduTestCase() {
  lateinit var action: AnAction
  override fun setUp() {
    super.setUp()
    action = ActionManager.getInstance().getAction("Educational.Educator.ExportStepikIds")
  }

  fun `test not available for local course`() {
    courseWithFiles {
      lesson { eduTask { } }
      section { lesson { eduTask { } } }
    }
    val presentation = myFixture.testAction(action)
    assertFalse("Action shouldn't be available for local course", presentation.isEnabledAndVisible)
  }

  fun `test export stepik ids`() {
    val course = courseWithFiles {
      lesson { eduTask { } }
      section { lesson { eduTask { } } }
    }

    val remoteCourse = convertToRemoteCourse(course)

    remoteCourse.generateUniqueIds()

    StudyTaskManager.getInstance(project).course = remoteCourse

    myFixture.testAction(action)
    val courseDir = EduUtils.getCourseDir(project)
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
  ]
}
    """.trimIndent()

    assertEquals(expectedFileContent, actualFileContent)
  }

  private fun convertToRemoteCourse(course: Course): RemoteCourse {
    val remoteCourse = RemoteCourse()
    remoteCourse.name = course.name
    remoteCourse.courseMode = CCUtils.COURSE_MODE
    remoteCourse.items = course.items
    return remoteCourse
  }

  private fun RemoteCourse.generateUniqueIds() {
    id = 1
    sections[0].id = 2
    visitLessons { lesson ->
      val section = lesson.section
      val sectionId = section?.id ?: 1
      lesson.id = 10 * sectionId + lesson.index
      lesson.unitId = lesson.id
      for (task in lesson.taskList) {
        task.stepId = 10 * lesson.id + task.index
      }
      true
    }
  }
}
