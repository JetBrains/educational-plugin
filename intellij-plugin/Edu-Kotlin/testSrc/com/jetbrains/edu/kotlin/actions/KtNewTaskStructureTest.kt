package com.jetbrains.edu.kotlin.actions

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = KotlinLanguage.INSTANCE

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.md")
      dir("src") {
        file("Task.kt")
      }
      dir("test") {
        file("Tests.kt")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("test") {
        file("Tests.kt")
      }
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.md")
      dir("src") {
        file("Main.kt")
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
      dir("src") {
        file("Main.kt")
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
        file("Main.kt")
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
        file("Main.kt")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )
}
