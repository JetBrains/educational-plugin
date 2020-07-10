package com.jetbrains.edu.go

import com.goide.GoLanguage
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.TaskType
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.learning.EduActionTestCase

class GoCreateTaskTest : EduActionTestCase() {
  fun `test module name generated correctly`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = GoLanguage.INSTANCE) {
      lesson {}
    }

    val lessonFile = findFile("lesson1")

    /**
     * CCCreateTask does not validate names.
     * TODO: find the way to write integration tests (validate names & create StudyItem)
     */
    val taskName = "Good Task 666 пробелы и кириллица"
    assertNull(GoCourseBuilder().validateItemName(taskName, TaskType))

    withMockCreateStudyItemUi(MockNewStudyItemUi(taskName)) {
      testAction(dataContext(lessonFile), CCCreateTask())
    }

    val goModFile = course.lessons[0].taskList[0].taskFiles["go.mod"] ?: error("go.mod should be generated for Go task")
    assertEquals("module good_task_666_пробелы_и_кириллица\n", goModFile.text)
  }

  fun `test forbidden task name`() {
    val taskName = "Bad name !@#"
    assertEquals("Name contains forbidden symbols", GoCourseBuilder().validateItemName(taskName, TaskType))
  }
}