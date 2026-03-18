package com.jetbrains.edu.cpp.actions

import com.intellij.lang.Language
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.cpp.CppConfigurator.Companion.MAIN_CPP
import com.jetbrains.edu.cpp.CppConfigurator.Companion.TASK_CPP
import com.jetbrains.edu.cpp.CppConfigurator.Companion.TEST_CPP
import com.jetbrains.edu.cpp.CppProjectSettings
import com.jetbrains.edu.cpp.CppTemplates.Companion.defaultExecutableTaskCMakeList
import com.jetbrains.edu.cpp.CppTemplates.Companion.defaultTestTaskCMakeList
import org.junit.Test
import com.jetbrains.cmake.CMakeListsFileType.FILE_NAME as CMAKE_LISTS_TXT

class CppGoogleTestNewTaskStructureTest : CCNewTaskStructureTestBase() {

  private val settings: CppProjectSettings get() = CppProjectSettings()

  override val language: Language get() = OCLanguage.getInstance()
  override val environment: String get() = "GoogleTest"

  @Test
  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file(CMAKE_LISTS_TXT, defaultTestTaskCMakeList.getText("global-lesson1-task1", settings.languageStandard))
      dir("src") {
        file(TASK_CPP)
      }
      dir("test") {
        file(TEST_CPP)
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
      dir("test") {
        file(TEST_CPP)
      }
    }
  )

  @Test
  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.md")
      file(CMAKE_LISTS_TXT, defaultExecutableTaskCMakeList.getText("global-lesson1-task1", settings.languageStandard))
      dir("src") {
        file(MAIN_CPP)
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
      file(CMAKE_LISTS_TXT, defaultExecutableTaskCMakeList.getText("global-lesson1-task1", settings.languageStandard))
      dir("src") {
        file(MAIN_CPP)
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
      file(CMAKE_LISTS_TXT, defaultExecutableTaskCMakeList.getText("global-lesson1-task1", settings.languageStandard))
      dir("src") {
        file(MAIN_CPP)
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
      file(CMAKE_LISTS_TXT, defaultExecutableTaskCMakeList.getText("global-lesson1-task1", settings.languageStandard))
      dir("src") {
        file(MAIN_CPP)
      }
    },
    taskStructureWithoutSources = {
      file("task.md")
    }
  )
}
