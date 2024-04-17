package com.jetbrains.edu.python.actions

import com.intellij.lang.Language
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.python.PythonLanguage
import org.junit.Test

class PyNewNewTaskStructureTest : CCNewTaskStructureTestBase() {
  override val language: Language get() = PythonLanguage.INSTANCE
  override val environment: String = "unittest"

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("__init__.py")
      file("task.py")
      dir("tests") {
        file("__init__.py")
        file("test_task.py")
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("tests") {
        file("__init__.py")
        file("test_task.py")
      }
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file("__init__.py")
      file("main.py")
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
      file("__init__.py")
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
      file("__init__.py")
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
      file("__init__.py")
      file("main.py")
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )
}
