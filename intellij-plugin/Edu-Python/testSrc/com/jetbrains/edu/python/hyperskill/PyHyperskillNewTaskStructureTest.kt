package com.jetbrains.edu.python.hyperskill

import com.intellij.lang.Language
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.learning.EduUtilsKt
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.python.PythonLanguage
import org.junit.Test

class PyHyperskillNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = PythonLanguage.INSTANCE
  override val courseProducer: () -> Course = ::HyperskillCourse

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    // Hyperskill python support is not available in Android Studio
    if (!EduUtilsKt.isAndroidStudio()) {
      withDefaultHtmlTaskDescription {
        super.runTestRunnable(context)
      }
    }
  }

  @Test
  fun `test create edu task`() {
    checkEduTaskCreation(
      fullTaskStructure = {
        file("task.html")
        file("task.py")
        dir("hstest") {
          file("tests.py")
        }
      },
      taskStructureWithoutSources = {
        file("task.html")
        dir("hstest") {
          file("tests.py")
        }
      }
    )
  }

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("main.py")
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

  @Test
  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("main.py")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  @Test
  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("main.py")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  @Test
  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("main.py")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
