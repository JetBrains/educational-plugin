package com.jetbrains.edu.cpp.actions

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.coursecreator.actions.CCCreateTask
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.cpp.*
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.fileTree

class CppCreateTaskTest : EduActionTestCase() {
  private val defaultSettings = CppProjectSettings()

  private fun `create task in empty lesson base`(environment: String, useFrameworkLesson: Boolean = false) {
    val course = courseWithFiles(
      language = OCLanguage.getInstance(),
      courseMode = CCUtils.COURSE_MODE,
      environment = environment
    ) {
      if (useFrameworkLesson)
        frameworkLesson("lesson")
      else
        lesson("lesson")
    }

    val lessonFile = findFile("lesson")

    withMockCreateStudyItemUi(MockNewStudyItemUi()) {
      testAction(dataContext(lessonFile), CCCreateTask())
    }
    val fileTree = fileTree {
      dir("lesson/task1") {
        dir("src") {
          file("task.cpp", getInternalTemplateText(CppBaseConfigurator.TASK_CPP))
        }
        dir("test") {
          file("test.cpp", when (environment) {
            "GoogleTest" -> getInternalTemplateText(CppBaseConfigurator.TEST_CPP)
            "Catch" -> getInternalTemplateText(CppCatchConfigurator.CATCH_TEST_CPP)
            else -> error("Test file undefined for environment `$environment`!")
          })
        }
        file(
          CMakeListsFileType.FILE_NAME,
          getExpectedTaskCMakeText(course, defaultSettings, "global-lesson-task1")
        )
        file("task.html")
      }
      dir("cmake") {
        when (environment) {
          "GoogleTest" -> {
            file("utils.cmake")
            file("googletest.cmake")
            file("googletest-download.cmake")
          }
          "Catch" -> {
            file("catch.cmake")
            file("utils.cmake")
          }
          else -> error("Content of the `cmake` directory undefined for environment `$environment`")
        }
      }
      file(CMakeListsFileType.FILE_NAME)
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  fun `test create task in empty lesson (Google Test)`() =
    `create task in empty lesson base`("GoogleTest")

  fun `test crete task in empty lesson (Catch Test)`() =
    `create task in empty lesson base`("Catch")

  fun `test create task in empty framework lesson (Google Test)`() =
    `create task in empty lesson base`("GoogleTest", true)

  fun `test crete task in empty framework lesson (Catch Test)`() =
    `create task in empty lesson base`("Catch", true)

  fun `test create second framework task`() {
    val taskText = """
      |#include<iostream>
      |
      |int main() {
      |  std::cout << "Hi, from Bugs Bunny!" << std::endl;
      |  return 0;
      |}
    """.trimMargin("|")

    val testText = """
      |//Image some tests here
    """.trimMargin("|")

    val cMakeListsTextGenerator: (String) -> String = { projectName ->
      """
        |cmake_minimum_required(VERSION 3.13)
        |project($projectName)
        |
        |set(CMAKE_CXX_STANDARD 14)
        |
        |add_executable(${'$'}{PROJECT_NAME}-run src/task.cpp)
        |add_executable(${'$'}{PROJECT_NAME}-test src/task.cpp test/test.cpp)
        |
        |configure_test_target(${'$'}{PROJECT_NAME}-test)
      """.trimMargin("|")
    }

    val course = courseWithFiles(
      language = OCLanguage.getInstance(),
      courseMode = CCUtils.COURSE_MODE,
      environment = "Catch" // environment isn't meter here
    ) {
      frameworkLesson("lesson") {
        eduTask("task1") {
          cppTaskFile("src/task.cpp", taskText)
          cppTaskFile("test/test.cpp", testText)
          taskFile(
            CMakeListsFileType.FILE_NAME,
            cMakeListsTextGenerator("global-lesson-task1")
          )
        }
      }
    }

    val lessonFile = findFile("lesson")

    withMockCreateStudyItemUi(MockNewStudyItemUi()) {
      testAction(dataContext(lessonFile), CCCreateTask())
    }
    val fileTree = fileTree {
      dir("lesson") {
        dir("task1") {
          dir("src") {
            file("task.cpp", taskText)
          }
          dir("test") {
            file("test.cpp", testText)
          }
          file(
            CMakeListsFileType.FILE_NAME,
            cMakeListsTextGenerator("global-lesson-task1")
          )
          file("task.html")
        }

        dir("task2") {
          dir("src") {
            file("task.cpp", taskText)
          }
          dir("test") {
            file("test.cpp", getInternalTemplateText(CppCatchConfigurator.CATCH_TEST_CPP))
          }
          file(
            CMakeListsFileType.FILE_NAME,
            cMakeListsTextGenerator("global-lesson-task2")
          )
          file("task.html")
        }
      }
      dir("cmake") {
        file("catch.cmake")
        file("utils.cmake")
      }
      file(CMakeListsFileType.FILE_NAME)
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot())
  }
}