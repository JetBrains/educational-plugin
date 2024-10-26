package com.jetbrains.edu.go

import com.goide.GoLanguage
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.testAction
import org.junit.Test

class GoCreateTaskTest : EduActionTestCase() {
  @Suppress("NonAsciiCharacters")
  @Test
  fun `test module name generated correctly`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = GoLanguage.INSTANCE) {
      lesson {}
    }

    val lessonFile = findFile("lesson1")

    val mockUi = MockNewStudyItemUi("Good Task 666 пробелы и кириллица")
    withMockCreateStudyItemUi(mockUi) {
      testAction(CCCreateTask.ACTION_ID, dataContext(lessonFile))
    }
    check(mockUi.errorMessage == null)

    val goModFile = course.lessons[0].taskList[0].taskFiles["go.mod"] ?: error("go.mod should be generated for Go task")
    assertEquals("module good_task_666_пробелы_и_кириллица\n", goModFile.text)
  }

  @Test
  fun `test forbidden task name`() {
    val course = courseWithFiles(courseMode = CourseMode.EDUCATOR, language = GoLanguage.INSTANCE) {
      lesson {}
    }

    val lessonFile = findFile("lesson1")

    val mockUi = MockNewStudyItemUi("Bad name !@#")
    withMockCreateStudyItemUi(mockUi) {
      testAction(CCCreateTask.ACTION_ID, dataContext(lessonFile))
    }

    assertEquals("Name contains forbidden symbols", mockUi.errorMessage)
    assertTrue(course.lessons[0].taskList.isEmpty())
  }
}
