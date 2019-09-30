package com.jetbrains.edu.cpp

import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.course.StepikCourse

/** Provides CMake file template information, where:
 * [mainCMakeList] - configures the course project, e.g. find all tasks `CMakeList.txt` files and adds them.
 * [taskCMakeList] - configures the task project.
 * [extraTopLevelFiles] - another depended on course type files that should be added to the top dir of the course.
 */
data class CppTemplates(
  val mainCMakeList: TemplateInfo,
  val taskCMakeList: TemplateInfo,
  val extraTopLevelFiles: List<TemplateInfo> = emptyList()
)

data class TemplateInfo(private val templateName: String, val generatedFileName: String) {
  fun getText(projectName: String = "", cppStandardLine: String = ""): String {
    val templateVariables = mapOf(
      "CMAKE_MINIMUM_REQUIRED_LINE" to cMakeMinimumRequired,
      "GTEST_VERSION" to CppConfigurator.GTEST_VERSION,
      "TEST_FRAMEWORK_DIR" to CppConfigurator.TEST_FRAMEWORK_DIR,
      EduNames.PROJECT_NAME to projectName,
      "CPP_STANDARD" to cppStandardLine
    )
    return GeneratorUtils.getInternalTemplateText(templateName, templateVariables)
  }
}

fun getCppTemplates(course: Course): CppTemplates =
  if (course is StepikCourse)
    CppTemplates(TemplateInfo("StepikMainCMakeList.txt", CMakeListsFileType.FILE_NAME),
                 TemplateInfo("StepikTaskCMakeList.txt", CMakeListsFileType.FILE_NAME),
                 listOf(
                   TemplateInfo("cmake_utils.cmake", GeneratorUtils.joinPaths("cmake", "utils.cmake"))
                 ))
  else
    CppTemplates(TemplateInfo("EduMainCMakeList.txt", CMakeListsFileType.FILE_NAME),
                 TemplateInfo("EduTaskCMakeList.txt", CMakeListsFileType.FILE_NAME),
                 listOf(
                   TemplateInfo("cmake_utils.cmake", GeneratorUtils.joinPaths("cmake", "utils.cmake")),
                   TemplateInfo("cmake_googletest.cmake", GeneratorUtils.joinPaths("cmake", "googletest.cmake")),
                   TemplateInfo("cmake_googletest-download.cmake", GeneratorUtils.joinPaths("cmake", "googletest-download.cmake")),
                   TemplateInfo("runTests.cpp", "run.cpp")
                 ))