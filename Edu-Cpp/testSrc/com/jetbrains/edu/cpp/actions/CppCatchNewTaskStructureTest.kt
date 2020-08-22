package com.jetbrains.edu.cpp.actions

import com.intellij.lang.Language
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.coursecreator.actions.create.CCNewTaskStructureTestBase
import com.jetbrains.edu.cpp.CppBaseConfigurator.Companion.MAIN_CPP
import com.jetbrains.edu.cpp.CppBaseConfigurator.Companion.TASK_CPP
import com.jetbrains.edu.cpp.CppBaseConfigurator.Companion.TEST_CPP
import com.jetbrains.edu.cpp.CppProjectSettings
import com.jetbrains.edu.cpp.CppTemplates.Companion.defaultExecutableTaskCMakeList
import com.jetbrains.edu.cpp.CppTemplates.Companion.defaultTestTaskCMakeList
import com.jetbrains.cmake.CMakeListsFileType.FILE_NAME as CMAKE_LISTS_TXT

class CppCatchNewTaskStructureTest : CCNewTaskStructureTestBase() {

  override val language: Language get() = OCLanguage.getInstance()
  override val settings: CppProjectSettings get() = CppProjectSettings()
  override val environment: String get() = "Catch"

  fun `test create edu task`() = checkEduTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file(CMAKE_LISTS_TXT, defaultTestTaskCMakeList.getText("global-lesson1-task1", settings.languageStandard))
      dir("src") {
        file(TASK_CPP)
      }
      dir("test") {
        file(TEST_CPP)
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
      dir("test") {
        file(TEST_CPP)
      }
    }
  )

  fun `test create output task`() = checkOutputTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file(CMAKE_LISTS_TXT, defaultExecutableTaskCMakeList.getText("global-lesson1-task1", settings.languageStandard))
      dir("src") {
        file(MAIN_CPP)
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
      file(CMAKE_LISTS_TXT, defaultExecutableTaskCMakeList.getText("global-lesson1-task1", settings.languageStandard))
      dir("src") {
        file(MAIN_CPP)
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create IDE task`() = checkIdeTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file(CMAKE_LISTS_TXT, defaultExecutableTaskCMakeList.getText("global-lesson1-task1", settings.languageStandard))
      dir("src") {
        file(MAIN_CPP)
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )

  fun `test create choice task`() = checkChoiceTaskCreation(
    fullTaskStructure = {
      file("task.html")
      file(CMAKE_LISTS_TXT, defaultExecutableTaskCMakeList.getText("global-lesson1-task1", settings.languageStandard))
      dir("src") {
        file(MAIN_CPP)
      }
    },
    taskStructureWithoutSources = {
      file("task.html")
    }
  )
}
