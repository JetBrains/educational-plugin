package com.jetbrains.edu.javascript.actions

import com.intellij.lang.Language
import com.intellij.lang.javascript.JavascriptLanguage
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import org.junit.Test

class JsNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = JavascriptLanguage.INSTANCE

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("task.js")
      dir("test") {
        file("test.js")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("test") {
        file("test.js")
      }
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("task.js")
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
      file("task.js")
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  @Test
  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("task.js")
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  @Test
  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("task.js")
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )
}
