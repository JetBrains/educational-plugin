package com.jetbrains.edu.cpp.actions

import com.intellij.lang.Language
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.cpp.CppProjectSettings

// TODO: check text of CMakeLists.txt files
class CppGoogleTestNewTaskStructureTest : CCNewTaskStructureTestBase() {

  override val language: Language get() = OCLanguage.getInstance()
  override val settings: Any get() = CppProjectSettings()
  override val environment: String get() = "GoogleTest"

  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("CMakeLists.txt")
      dir("src") {
        file("task.cpp")
      }
      dir("test") {
        file("test.cpp")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("test") {
        file("test.cpp")
      }
    }
  )

  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("CMakeLists.txt")
      dir("src") {
        file("main.cpp")
      }
      dir("test") {
        file("output.txt")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("test") {
        file("output.txt")
      }
    }
  )

  fun `test create theory task`() = checkTheoryTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("CMakeLists.txt")
      dir("src") {
        file("main.cpp")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("CMakeLists.txt")
      dir("src") {
        file("main.cpp")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file("CMakeLists.txt")
      dir("src") {
        file("main.cpp")
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
