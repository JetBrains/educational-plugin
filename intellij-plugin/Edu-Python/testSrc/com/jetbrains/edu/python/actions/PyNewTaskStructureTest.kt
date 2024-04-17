package com.jetbrains.edu.python.actions

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.python.PythonLanguage
import org.junit.Test

class PyNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = PythonLanguage.INSTANCE

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("task.py")
      file("tests.py")
    },
    taskStructureWithoutSources = {
      file("task.md")
      file("tests.py")
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("main.py")
      file("output.txt")
      file("input.txt")
    },
    taskStructureWithoutSources = {
      file("task.md")
      file("output.txt")
      file("input.txt")
    }
  )

  @Test
  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("main.py")
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  @Test
  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("main.py")
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )

  @Test
  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("main.py")
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )
}
