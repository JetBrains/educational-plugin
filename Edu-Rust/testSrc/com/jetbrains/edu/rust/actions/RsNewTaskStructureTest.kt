package com.jetbrains.edu.rust.actions

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.rust.RsProjectSettings
import org.rust.lang.RsLanguage

class RsNewTaskStructureTest : CCNewTaskStructureTestBase() {

  override val language: Language get() = RsLanguage
  override val settings: Any get() = RsProjectSettings()

  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("Cargo.toml")
      dir("src") {
        file("lib.rs")
        file("main.rs")
      }
      dir("tests") {
        file("tests.rs")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("tests") {
        file("tests.rs")
      }
    }
  )

  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("Cargo.toml")
      dir("src") {
        file("main.rs")
      }
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
      file("Cargo.toml")
      dir("src") {
        file("main.rs")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("Cargo.toml")
      dir("src") {
        file("main.rs")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("Cargo.toml")
      dir("src") {
        file("main.rs")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
