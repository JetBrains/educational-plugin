package com.jetbrains.edu.java.actions

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import org.junit.Test

class JNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = JavaLanguage.INSTANCE

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.md")
      dir("src") {
        file("Task.java")
      }
      dir("test") {
        file("Tests.java")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("test") {
        file("Tests.java")
      }
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.md")
      dir("src") {
        file("Main.java")
      }
      dir("test") {
        file("input.txt")
        file("output.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("test") {
        file("input.txt")
        file("output.txt")
      }
    }
  )

  @Test
  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.md")
      dir("src") {
        file("Main.java")
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
      dir("src") {
        file("Main.java")
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
      dir("src") {
        file("Main.java")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )
}
