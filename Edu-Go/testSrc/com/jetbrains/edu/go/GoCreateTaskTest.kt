package com.jetbrains.edu.go

import com.goide.GoLanguage
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.StudyItemType.TASK_TYPE
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.EduActionTestCase

class GoCreateTaskTest : EduActionTestCase() {
  fun `test module name generated correctly`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = GoLanguage.INSTANCE) {
      lesson {}
    }

    val lessonFile = findFile("lesson1")

    val mockUi = MockNewStudyItemUi("Good Task 666 пробелы и кириллица")
    withMockCreateStudyItemUi(mockUi) {
      testAction(dataContext(lessonFile), CCCreateTask())
    }
    check(mockUi.errorMessage == null)

    val goModFile = course.lessons[0].taskList[0].taskFiles["go.mod"] ?: error("go.mod should be generated for Go task")
    assertEquals("module good_task_666_пробелы_и_кириллица\n", goModFile.text)
  }

  fun `test forbidden task name`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = GoLanguage.INSTANCE) {
      lesson {}
    }

    val lessonFile = findFile("lesson1")

    val mockUi = MockNewStudyItemUi("Bad name !@#")
    withMockCreateStudyItemUi(mockUi) {
      testAction(dataContext(lessonFile), CCCreateTask())
    }

    assertEquals("Name contains forbidden symbols", mockUi.errorMessage)
    assertTrue(course.lessons[0].taskList.isEmpty())
  }
}
