package com.jetbrains.edu.javascript.hyperskill

import com.intellij.lang.Language
import com.intellij.lang.javascript.JavascriptLanguage
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.HyperskillCourse

class JsHyperskillNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = JavascriptLanguage.INSTANCE
  override val courseProducer: () -> Course = ::HyperskillCourse

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withDefaultHtmlTaskDescription {
      super.runTestRunnable(context)
    }
  }

  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("task.js")
      dir("hstest") {
        file("test.js")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("hstest") {
        file("test.js")
      }
    }
  )

  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("task.js")
      dir("hstest") {
        file("output.txt")
        file("input.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("hstest") {
        file("output.txt")
        file("input.txt")
      }
    }
  )

  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("task.js")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("task.js")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("task.js")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
