package com.jetbrains.edu.scala.actions

import com.jetbrains.edu.coursecreator.actions.placeholder.CCAddAnswerPlaceholderActionTestBase
import com.jetbrains.edu.jvm.JdkProjectSettings
import com.jetbrains.edu.learning.courseFormat.CourseMode
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaGradleAnswerPlaceholderActionTest : CCAddAnswerPlaceholderActionTestBase() {

  fun `test placeholder text`() {
    val course = courseWithFiles(courseMode = CourseMode.COURSE_MODE, language = ScalaLanguage.INSTANCE,
                                 environment = "Gradle", settings = JdkProjectSettings.emptySettings()) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt", DEFAULT_TASK_TEXT)
        }
      }
    }

    val taskFile = course.lessons[0].taskList[0].taskFiles["Task.kt"]!!
    val taskFileExpected = copy(taskFile)
    taskFileExpected.createExpectedPlaceholder(0, "/* TODO */", "/* TODO */")

    doTest("lesson1/task1/Task.kt", CCTestAddAnswerPlaceholder(), taskFile, taskFileExpected)
  }

}