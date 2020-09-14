package com.jetbrains.edu.kotlin.actions

import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.placeholder.CCAddAnswerPlaceholderActionTestBase
import com.jetbrains.edu.jvm.JdkProjectSettings
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtAddAnswerPlaceholderActionTest : CCAddAnswerPlaceholderActionTestBase() {

  fun `test placeholder text`() {
    val course = courseWithFiles(courseMode = CCUtils.COURSE_MODE, language = KotlinLanguage.INSTANCE,
                                 settings = JdkProjectSettings.emptySettings()) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", DEFAULT_TASK_TEXT)
        }
      }
    }

    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    taskFileExpected.createExpectedPlaceholder(0, "TODO()", "TODO()")

    doTest("lesson1/task1/Task.kt", CCTestAddAnswerPlaceholder(), taskFile, taskFileExpected)
  }

}