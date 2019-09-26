package com.jetbrains.edu.cpp

import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.course.StepikCourse

/** Provides CMake file template names, where:
 * [mainCMakeList] - configures the course project, e.g. find all tasks `CMakeList.txt` files and adds them.
 * [taskCMakeList] - configures the task project.
 * [extraTopLevelFiles] - another depended on course type files that should be added to the top dir of the course.
 */
data class CppTemplates(
  val mainCMakeList: TemplateInfo,
  val taskCMakeList: TemplateInfo,
  val extraTopLevelFiles: List<TemplateInfo> = emptyList()
) {
  data class TemplateInfo(val templateName: String, val fileName: String, val dataKeys: List<String> = emptyList()) {
    fun getText(dataProvider: (String) -> String = { "" }): String {
      val templateVariables = dataKeys.map { it to dataProvider(it) }.toMap()
      return GeneratorUtils.getInternalTemplateText(templateName, templateVariables)
    }
  }

  companion object {
    const val CMAKE_MINIMUM_REQUIRED_LINE_KAY: String = "CMAKE_MINIMUM_REQUIRED_LINE"
    const val PROJECT_NAME_KEY = EduNames.PROJECT_NAME
    const val CPP_STANDARD_LINE_KEY = "CPP_STANDARD"
    const val GTEST_VERSION_KEY = "GTEST_VERSION"
  }
}

fun getCppTemplates(course: Course): CppTemplates =
  if (course is StepikCourse)
    CppTemplates(CppTemplates.TemplateInfo("StepikMainCMakeList.txt", CMakeListsFileType.FILE_NAME,
                                           listOf(
                                             CppTemplates.CMAKE_MINIMUM_REQUIRED_LINE_KAY,
                                             CppTemplates.PROJECT_NAME_KEY
                                           )),
                 CppTemplates.TemplateInfo("StepikTaskCMakeList.txt", CMakeListsFileType.FILE_NAME,
                                           listOf(
                                             CppTemplates.CMAKE_MINIMUM_REQUIRED_LINE_KAY,
                                             CppTemplates.PROJECT_NAME_KEY,
                                             CppTemplates.CPP_STANDARD_LINE_KEY
                                           )))
  else
    CppTemplates(CppTemplates.TemplateInfo("EduMainCMakeList.txt", CMakeListsFileType.FILE_NAME,
                                           listOf(
                                             CppTemplates.CMAKE_MINIMUM_REQUIRED_LINE_KAY,
                                             CppTemplates.PROJECT_NAME_KEY
                                           )),
                 CppTemplates.TemplateInfo("EduTaskCMakeList.txt", CMakeListsFileType.FILE_NAME,
                                           listOf(
                                             CppTemplates.CMAKE_MINIMUM_REQUIRED_LINE_KAY,
                                             CppTemplates.PROJECT_NAME_KEY,
                                             CppTemplates.CPP_STANDARD_LINE_KEY
                                           )),
                 listOf(
                   CppTemplates.TemplateInfo("EduTestCMakeList.txt.in", "${CMakeListsFileType.FILE_NAME}.in",
                                             listOf(
                                               CppTemplates.CMAKE_MINIMUM_REQUIRED_LINE_KAY,
                                               CppTemplates.GTEST_VERSION_KEY
                                             )),
                   CppTemplates.TemplateInfo("runTests.cpp", "run.cpp")
                 ))