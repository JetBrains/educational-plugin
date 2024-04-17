package com.jetbrains.edu.rust.actions

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import org.junit.Test
import org.rust.lang.RsLanguage

class RsNewTaskStructureTest : CCNewTaskStructureTestBase() {

  override val language: Language get() = RsLanguage

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.md")
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
      file("task.md")
      dir("tests") {
        file("tests.rs")
      }
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("Cargo.toml")
      dir("src") {
        file("main.rs")
      }
      dir("tests") {
        file("output.txt")
        file("input.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("tests") {
        file("output.txt")
        file("input.txt")
      }
    }
  )

  @Test
  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("Cargo.toml")
      dir("src") {
        file("main.rs")
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
      file("Cargo.toml")
      dir("src") {
        file("main.rs")
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
      file("Cargo.toml")
      dir("src") {
        file("main.rs")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )
}
