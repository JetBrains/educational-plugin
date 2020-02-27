package com.jetbrains.edu.coursecreator.actions.create

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.PlainTextLanguage

class CCNewTaskStructureTest : CCNewTaskStructureTestBase() {

  override val language: Language get() = PlainTextLanguage.INSTANCE
  override val settings: Any get() = Unit

  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("Task.txt")
      dir("tests") {
        file("Tests.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("tests") {
        file("Tests.txt")
      }
    }
  )

  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("Task.txt")
      dir("tests") {
        file("output.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("tests") {
        file("output.txt")
      }
    }
  )

  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("Task.txt")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("Task.txt")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("Task.txt")
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
