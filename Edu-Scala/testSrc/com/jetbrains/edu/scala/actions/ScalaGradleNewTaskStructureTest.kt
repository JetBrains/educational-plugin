package com.jetbrains.edu.scala.actions

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.jvm.JdkProjectSettings
import org.jetbrains.plugins.scala.ScalaLanguage

class ScalaGradleNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = ScalaLanguage.INSTANCE
  override val settings: Any get() = JdkProjectSettings.emptySettings()
  override val environment: String = "Gradle"

  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.md")
      dir("src") {
        file("Task.scala")
      }
      dir("test") {
        file("Test.scala")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("test") {
        file("Test.scala")
      }
    }
  )

  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.md")
      dir("src") {
        file("Main.scala")
      }
      dir("test") {
        file("output.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("test") {
        file("output.txt")
      }
    }
  )

  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.md")
      dir("src") {
        file("Main.scala")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.md")
      dir("src") {
        file("Main.scala")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.md")
      dir("src") {
        file("Main.scala")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )
}
