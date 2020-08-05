package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.configurators.FakeGradleHyperskillConfigurator
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask

class HyperskillFindCodeTaskFileTest : EduTestCase() {
  fun `test find the only file`() {
    val task = CodeTask("task1")
    val answerFile = TaskFile("src/Task.kt", "")

    task.addTaskFile(answerFile)
    doTest(task, answerFile)
  }

  fun `test find Main file`() {
    val task = CodeTask("task1")
    val answerFile = TaskFile("src/Main.kt", "")

    task.addTaskFile(TaskFile("src/Task.kt", ""))
    task.addTaskFile(answerFile)
    doTest(task, answerFile)
  }

  fun `test find first visible & educator-created file`() {
    val task = CodeTask("task1")
    val answerFile = TaskFile("src/Task.kt", "")

    task.addTaskFile(TaskFile("src/Foo.kt", "", true, true))
    task.addTaskFile(answerFile)
    task.addTaskFile(TaskFile("src/Bar.kt", "", false, false))
    task.addTaskFile(TaskFile("src/Baz.kt", ""))
    doTest(task, answerFile)
  }

  fun `test unable to find file`() {
    val task = CodeTask("task1")
    task.addTaskFile(TaskFile("src/Foo.kt", "", false, false))
    task.addTaskFile(TaskFile("src/Bar.kt", "", true, true))
    doTest(task, null)
  }

  private fun doTest(task: CodeTask, answerFile: TaskFile?) {
    val configurator = FakeGradleHyperskillConfigurator()
    val codeTaskFile = configurator.getCodeTaskFile(project, task)
    assertEquals(answerFile, codeTaskFile)
  }
}