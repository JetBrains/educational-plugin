package com.jetbrains.edu.cpp

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.jetbrains.cidr.cpp.cmake.projectWizard.CLionProjectWizardUtils
import com.jetbrains.cidr.cpp.toolchains.CMake
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.course.StepikCourse
import java.lang.IllegalStateException

private const val GTEST_VERSION = "release-1.8.1"
const val TEST_FRAMEWORK_DIR = "test-framework"

private val cMakeMinimumRequired: String by lazy {
  val cMakeVersionExtractor = {
    CLionProjectWizardUtils.getCMakeMinimumRequiredLine(CMake.readCMakeVersion(CPPToolchains.getInstance().defaultToolchain))
  }

  val progressManager = ProgressManager.getInstance()

  if (progressManager.hasProgressIndicator()) {
    progressManager.runProcess(cMakeVersionExtractor, null)
  }
  else {
    progressManager.run(object : Task.WithResult<String, Nothing>(null, "Getting CMake Minimum Required Version", false) {
      override fun compute(indicator: ProgressIndicator) = cMakeVersionExtractor()
    })
  }
}

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
  private fun getTemplateVariables(projectName: String, cppStandardLine: String) = mapOf(
    "CMAKE_MINIMUM_REQUIRED_LINE" to cMakeMinimumRequired,
    "GTEST_VERSION" to GTEST_VERSION,
    "TEST_FRAMEWORK_DIR" to TEST_FRAMEWORK_DIR,
    EduNames.PROJECT_NAME to projectName,
    "CPP_STANDARD" to cppStandardLine
  )

  fun getText(projectName: String = "", cppStandardLine: String = ""): String =
    GeneratorUtils.getInternalTemplateText(templateName, getTemplateVariables(projectName, cppStandardLine))
}

fun getCppTemplates(course: Course): CppTemplates =
  when {
    course is StepikCourse ->
      CppTemplates(TemplateInfo("StepikMainCMakeList.txt", CMakeListsFileType.FILE_NAME),
                   TemplateInfo("StepikTaskCMakeList.txt", CMakeListsFileType.FILE_NAME),
                   listOf(
                     TemplateInfo("stepik_cmake_utils.cmake", GeneratorUtils.joinPaths("cmake", "utils.cmake"))
                   ))
    course.environment == "GoogleTest" ->
      CppTemplates(TemplateInfo("EduMainCMakeList.txt", CMakeListsFileType.FILE_NAME),
                   TemplateInfo("EduTaskCMakeList.txt", CMakeListsFileType.FILE_NAME),
                   listOf(
                     TemplateInfo("cmake_utils.cmake", GeneratorUtils.joinPaths("cmake", "utils.cmake")),
                     TemplateInfo("cmake_googletest.cmake",
                                  GeneratorUtils.joinPaths("cmake", "googletest.cmake")),
                     TemplateInfo("cmake_googletest-download.cmake",
                                  GeneratorUtils.joinPaths("cmake", "googletest-download.cmake"))
                   ))
    course.environment == "Catch" ->
      CppTemplates(TemplateInfo("catch_CMakeLists.txt", CMakeListsFileType.FILE_NAME),
                   TemplateInfo("EduTaskCMakeList.txt", CMakeListsFileType.FILE_NAME),
                   listOf(
                     TemplateInfo("catch_cmake_utils.cmake", GeneratorUtils.joinPaths("cmake", "utils.cmake")),
                     TemplateInfo("catch_cmake_catch.cmake", GeneratorUtils.joinPaths("cmake", "catch.cmake")),
                     TemplateInfo("catch_run.cpp", "run.cpp")
                   ))
    else ->
      throw IllegalStateException("Course must be Stepik type or have one of these environments: GoogleTest, Catch")
  }