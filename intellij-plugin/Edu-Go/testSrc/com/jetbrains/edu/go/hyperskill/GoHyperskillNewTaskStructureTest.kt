package com.jetbrains.edu.go.hyperskill

import com.goide.GoLanguage
import com.intellij.lang.Language
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.go.GoConfigurator.Companion.GO_MOD
import com.jetbrains.edu.go.GoConfigurator.Companion.MAIN_GO
import com.jetbrains.edu.go.GoConfigurator.Companion.TASK_GO
import com.jetbrains.edu.go.GoConfigurator.Companion.TEST_GO
import com.jetbrains.edu.learning.EduNames.TEST
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.junit.Test

class GoHyperskillNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = GoLanguage.INSTANCE
  override val courseProducer: () -> Course = ::HyperskillCourse

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withDefaultHtmlTaskDescription {
      super.runTestRunnable(context)
    }
  }

  /** [com.jetbrains.edu.go.GoCourseBuilder.getTestTaskTemplates] */
  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file(GO_MOD)
      dir("main") {
        file(MAIN_GO)
      }
      file(TASK_GO)
      dir(TEST) {
        file(TEST_GO)
      }
    },
    taskStructureWithoutSources = {
      dir(TEST) {
        file(TEST_GO)
      }
      file("task.html")
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file(GO_MOD)
      file(MAIN_GO)
      file("task.html")
      dir(TEST) {
        file("output.txt")
        file("input.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir(TEST) {
        file("output.txt")
        file("input.txt")
      }
    }
  )

  @Test
  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file(GO_MOD)
      file(MAIN_GO)
      file("task.html")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  @Test
  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file(GO_MOD)
      file(MAIN_GO)
      file("task.html")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  @Test
  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file(GO_MOD)
      file(MAIN_GO)
      file("task.html")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
