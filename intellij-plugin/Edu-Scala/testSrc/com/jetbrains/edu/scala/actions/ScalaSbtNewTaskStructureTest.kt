package com.jetbrains.edu.scala.actions

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import org.jetbrains.plugins.scala.ScalaLanguage
import org.junit.Test

class ScalaSbtNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = ScalaLanguage.INSTANCE
  override val environment: String = "sbt"

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("build.sbt")
      dir("src") {
        file("Task.scala")
      }
      dir("test") {
        file("TestSpec.scala")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("test") {
        file("TestSpec.scala")
      }
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("build.sbt")
      dir("src") {
        file("Main.scala")
      }
      dir("test") {
        file("output.txt")
        file("input.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("test") {
        file("output.txt")
        file("input.txt")
      }
    }
  )

  @Test
  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("build.sbt")
      dir("src") {
        file("Main.scala")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  @Test
  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("build.sbt")
      dir("src") {
        file("Main.scala")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  @Test
  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("build.sbt")
      dir("src") {
        file("Main.scala")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )
}
