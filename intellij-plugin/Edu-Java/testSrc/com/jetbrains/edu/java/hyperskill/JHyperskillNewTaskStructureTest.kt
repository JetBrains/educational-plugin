package com.jetbrains.edu.java.hyperskill

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.intellij.util.ThrowableRunnable
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import org.junit.Test

class JHyperskillNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = JavaLanguage.INSTANCE
  override val courseProducer: () -> Course = ::HyperskillCourse

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withDefaultHtmlTaskDescription {
      super.runTestRunnable(context)
    }
  }

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Task.java")
      }
      dir("test") {
        file("Tests.java")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("test") {
        file("Tests.java")
      }
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.java")
      }
      dir("test") {
        file("input.txt")
        file("output.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("test") {
        file("input.txt")
        file("output.txt")
      }
    }
  )

  @Test
  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.html")
      dir("src") {
        file("Main.java")
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
        file("Main.java")
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
        file("Main.java")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
