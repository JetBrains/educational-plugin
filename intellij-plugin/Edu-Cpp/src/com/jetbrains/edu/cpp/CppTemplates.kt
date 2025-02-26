package com.jetbrains.edu.cpp

import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.cpp.CMakeConstants.CMAKE_CATCH
import com.jetbrains.edu.cpp.CMakeConstants.CMAKE_DIRECTORY
import com.jetbrains.edu.cpp.CMakeConstants.CMAKE_GOOGLE_TEST
import com.jetbrains.edu.cpp.CMakeConstants.CMAKE_GOOGLE_TEST_DOWNLOAD
import com.jetbrains.edu.cpp.CMakeConstants.CMAKE_UTILS
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikCourse
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

object CMakeConstants {
  const val CMAKE_DIRECTORY = "cmake"
  const val CMAKE_UTILS = "utils.cmake"
  const val CMAKE_GOOGLE_TEST = "googletest.cmake"
  const val CMAKE_GOOGLE_TEST_DOWNLOAD = "googletest-download.cmake"
  const val CMAKE_CATCH = "catch.cmake"
}

/** Provides CMake file template information, where:
 * [mainCMakeList] - configures the course project, e.g. find all tasks `CMakeLists.txt` files and adds them.
 * [testTaskCMakeList] - configures the task project with tests.
 * [executableTaskCMakeList] - configure the task executable project.
 * [extraTopLevelFiles] - another depended on course type files that should be added to the top dir of the course.
 */
data class CppTemplates(
  val mainCMakeList: TemplateInfo = defaultMainCMakeList,
  val testTaskCMakeList: TemplateInfo = defaultTestTaskCMakeList,
  val executableTaskCMakeList: TemplateInfo = defaultExecutableTaskCMakeList,
  val extraTopLevelFiles: List<TemplateInfo> = defaultExtraTopLevelFiles
) {
  companion object {
    val defaultMainCMakeList: TemplateInfo = TemplateInfo("CMakeLists.txt", CMakeListsFileType.FILE_NAME)
    val defaultTestTaskCMakeList: TemplateInfo = TemplateInfo("task.testable.CMakeLists.txt", CMakeListsFileType.FILE_NAME)
    val defaultExecutableTaskCMakeList: TemplateInfo = TemplateInfo("task.executable.CMakeLists.txt", CMakeListsFileType.FILE_NAME)
    val defaultExtraTopLevelFiles: List<TemplateInfo> = listOf(
      TemplateInfo("cmake.utils.cmake", GeneratorUtils.joinPaths("cmake", "utils.cmake"))
    )
  }
}

data class TemplateInfo(private val templateName: String, val generatedFileName: String) {
  private fun getTemplateVariables(projectName: String, cppStandardLine: String) = mapOf(
    CMAKE_MINIMUM_REQUIRED_LINE_KEY to CMAKE_MINIMUM_REQUIRED_LINE_VALUE,
    CMAKE_PROJECT_NAME_KEY to projectName,
    CMAKE_CPP_STANDARD_KEY to cppStandardLine,

    GTEST_VERSION_KEY to GTEST_VERSION_VALUE,
    GTEST_SOURCE_DIR_KEY to GTEST_SOURCE_DIR_VALUE,
    GTEST_BUILD_DIR_KEY to GTEST_BUILD_DIR_VALUE,

    CATCH_HEADER_URL_KEY to CATCH_HEADER_URL_VALUE,

    TEST_FRAMEWORKS_BASE_DIR_KEY to TEST_FRAMEWORKS_BASE_DIR_VALUE
  )

  fun getText(projectName: String = "", cppStandardLine: String = ""): String =
    GeneratorUtils.getInternalTemplateText(templateName, getTemplateVariables(projectName, cppStandardLine))
}

fun getCppTemplates(course: Course): CppTemplates =
  when {
    course is StepikCourse ->
      CppTemplates(
        testTaskCMakeList = CppTemplates.defaultExecutableTaskCMakeList
      )
    course is HyperskillCourse -> CppTemplates(testTaskCMakeList = TemplateInfo("hyperskill.task.executable.CMakeLists.txt", CMakeListsFileType.FILE_NAME))
    course.environment == "GoogleTest" ->
      CppTemplates(
        mainCMakeList = TemplateInfo("gtest.CMakeLists.txt", CMakeListsFileType.FILE_NAME),
        extraTopLevelFiles = listOf(
          TemplateInfo("gtest.cmake.utils.cmake", GeneratorUtils.joinPaths(CMAKE_DIRECTORY, CMAKE_UTILS)),
          TemplateInfo("gtest.cmake.googletest.cmake", GeneratorUtils.joinPaths(CMAKE_DIRECTORY, CMAKE_GOOGLE_TEST)),
          TemplateInfo("gtest.cmake.googletest-download.cmake", GeneratorUtils.joinPaths(CMAKE_DIRECTORY, CMAKE_GOOGLE_TEST_DOWNLOAD)))
      )
    course.environment == "Catch" ->
      CppTemplates(
        mainCMakeList = TemplateInfo("catch.CMakeLists.txt", CMakeListsFileType.FILE_NAME),
        extraTopLevelFiles = listOf(
          TemplateInfo("catch.cmake.utils.cmake", GeneratorUtils.joinPaths(CMAKE_DIRECTORY, CMAKE_UTILS)),
          TemplateInfo("catch.cmake.catch.cmake", GeneratorUtils.joinPaths(CMAKE_DIRECTORY, CMAKE_CATCH)))
      )
    else ->
      throw IllegalStateException("Course must be Stepik or Hyperskill type or have one of these environments: GoogleTest, Catch")
  }