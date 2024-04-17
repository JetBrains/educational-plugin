package com.jetbrains.edu.cpp.actions

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.edu.coursecreator.actions.create.MockNewStudyItemUi
import com.jetbrains.edu.coursecreator.actions.studyItem.CCCreateTask
import com.jetbrains.edu.coursecreator.ui.withMockCreateStudyItemUi
import com.jetbrains.edu.cpp.CppCatchCourseBuilder
import com.jetbrains.edu.cpp.CppConfigurator.Companion.TASK_CPP
import com.jetbrains.edu.cpp.CppConfigurator.Companion.TEST_CPP
import com.jetbrains.edu.cpp.CppGTestCourseBuilder
import com.jetbrains.edu.cpp.CppProjectSettings
import com.jetbrains.edu.cpp.getExpectedTaskCMakeText
import com.jetbrains.edu.learning.EduActionTestCase
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import com.jetbrains.edu.learning.fileTree
import com.jetbrains.edu.learning.testAction
import org.junit.Test
import com.jetbrains.cmake.CMakeListsFileType.FILE_NAME as CMAKE_LISTS_TXT

class CppCreateTaskTest : EduActionTestCase() {
  private val defaultSettings = CppProjectSettings()

  private fun createTaskInEmptyLessonTestBase(environment: String, useFrameworkLesson: Boolean = false) {
    val course = courseWithFiles(
      language = OCLanguage.getInstance(),
      courseMode = CourseMode.EDUCATOR,
      environment = environment
    ) {
      if (useFrameworkLesson)
        frameworkLesson("lesson")
      else
        lesson("lesson")
    }

    val lessonFile = findFile("lesson")

    withMockCreateStudyItemUi(MockNewStudyItemUi()) {
      testAction(CCCreateTask.ACTION_ID, dataContext(lessonFile))
    }
    val fileTree = fileTree {
      dir("lesson/task1") {
        dir("src") {
          file(TASK_CPP, getInternalTemplateText(TASK_CPP))
        }
        dir("test") {
          file(TEST_CPP, when (environment) {
            "GoogleTest" -> getInternalTemplateText(CppGTestCourseBuilder.TEST_TEMPLATE_NAME)
            "Catch" -> getInternalTemplateText(CppCatchCourseBuilder.TEST_TEMPLATE_NAME)
            else -> error("Test file undefined for environment `$environment`!")
          })
        }
        file(CMAKE_LISTS_TXT, getExpectedTaskCMakeText(course, defaultSettings, "global-lesson-task1"))
        file("task.md")
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
      file(CMAKE_LISTS_TXT)
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot())
  }

  @Test
  fun `test create task in empty lesson (Google Test)`() =
    createTaskInEmptyLessonTestBase("GoogleTest")

  @Test
  fun `test crete task in empty lesson (Catch Test)`() =
    createTaskInEmptyLessonTestBase("Catch")

  @Test
  fun `test create task in empty framework lesson (Google Test)`() =
    createTaskInEmptyLessonTestBase("GoogleTest", true)

  @Test
  fun `test crete task in empty framework lesson (Catch Test)`() =
    createTaskInEmptyLessonTestBase("Catch", true)

  @Test
  fun `test create second framework task`() {
    val taskText = """
      |#include<iostream>
      |
      |int main() {
      |  std::cout << "Hi, from Bugs Bunny!" << std::endl;
      |  return 0;
      |}
    """.trimMargin()

    val testText = """
      |//Image some tests here
    """.trimMargin()

    val cMakeListsTextGenerator: (String) -> String = { projectName ->
      """
        |cmake_minimum_required(VERSION 3.13)
        |project($projectName)
        |
        |set(CMAKE_CXX_STANDARD 14)
        |
        |add_executable(${'$'}{PROJECT_NAME}-run src/$TASK_CPP)
        |add_executable(${'$'}{PROJECT_NAME}-test src/$TASK_CPP test/$TEST_CPP)
        |
        |configure_test_target(${'$'}{PROJECT_NAME}-test)
      """.trimMargin()
    }

    courseWithFiles(
      language = OCLanguage.getInstance(),
      courseMode = CourseMode.EDUCATOR,
      environment = "Catch" // Environment doesn't matter here
    ) {
      frameworkLesson("lesson") {
        eduTask("task1") {
          cppTaskFile("src/$TASK_CPP", taskText)
          cppTaskFile("test/$TEST_CPP", testText)
          taskFile(
            CMAKE_LISTS_TXT,
            cMakeListsTextGenerator("global-lesson-task1")
          )
        }
      }
    }

    val lessonFile = findFile("lesson")

    withMockCreateStudyItemUi(MockNewStudyItemUi()) {
      testAction(CCCreateTask.ACTION_ID, dataContext(lessonFile))
    }
    val fileTree = fileTree {
      dir("lesson") {
        dir("task1") {
          dir("src") {
            file(TASK_CPP, taskText)
          }
          dir("test") {
            file(TEST_CPP, testText)
          }
          file(
            CMAKE_LISTS_TXT,
            cMakeListsTextGenerator("global-lesson-task1")
          )
          file("task.md")
        }

        dir("task2") {
          dir("src") {
            file(TASK_CPP, taskText)
          }
          dir("test") {
            file(TEST_CPP, getInternalTemplateText(CppCatchCourseBuilder.TEST_TEMPLATE_NAME))
          }
          file(
            CMAKE_LISTS_TXT,
            cMakeListsTextGenerator("global-lesson-task2")
          )
          file("task.md")
        }
      }
      dir("cmake") {
        file("catch.cmake")
        file("utils.cmake")
      }
      file(CMAKE_LISTS_TXT)
    }
    fileTree.assertEquals(LightPlatformTestCase.getSourceRoot())
  }
}
