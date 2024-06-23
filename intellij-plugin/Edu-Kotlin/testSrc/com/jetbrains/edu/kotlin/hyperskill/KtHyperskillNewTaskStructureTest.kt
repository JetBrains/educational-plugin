package com.jetbrains.edu.kotlin.hyperskill

import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtHyperskillNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = KotlinLanguage.INSTANCE
  override val courseProducer: () -> Course = ::HyperskillCourse

  override fun runTestRunnable(testRunnable: ThrowableRunnable<Throwable>) {
    // https://youtrack.jetbrains.com/issue/EDU-6934
    if (ApplicationInfo.getInstance().build < BUILD_241) {
      withDefaultHtmlTaskDescription {
        super.runTestRunnable(testRunnable)
      }
    }
  }

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Task.kt")
      }
      dir("test") {
        file("Tests.kt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("test") {
        file("Tests.kt")
      }
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.kt")
      }
      dir("test") {
        file("output.txt")
        file("input.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("test") {
        file("output.txt")
        file("input.txt")
      }
    }
  )

  @Test
  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.kt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  @Test
  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.kt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  @Test
  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.kt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  companion object {
    private val BUILD_241 = BuildNumber.fromString("241")!!
  }
}
