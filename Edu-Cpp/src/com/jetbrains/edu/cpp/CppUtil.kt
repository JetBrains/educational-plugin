package com.jetbrains.edu.cpp

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.WithResult
import com.jetbrains.cidr.cpp.cmake.projectWizard.CLionProjectWizardUtils
import com.jetbrains.cidr.cpp.toolchains.CMake
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

private const val CMAKE_MINIMUM_REQUIRED_LINE = "CMAKE_MINIMUM_REQUIRED_LINE"
private const val CPP_STANDARD_LINE = "CPP_STANDARD"
private const val GTEST_VERSION = "GTEST_VERSION"

private val cMakeMinimumRequired: String by lazy {
  val cMakeVersionExtractor = {
    CLionProjectWizardUtils.getCMakeMinimumRequiredLine(CMake.readCMakeVersion(CPPToolchains.getInstance().defaultToolchain))
  }

  val progressManager = ProgressManager.getInstance()

  if (progressManager.hasProgressIndicator()) {
    progressManager.runProcess(cMakeVersionExtractor, null)
  }
  else {
    progressManager.run(object : WithResult<String, Nothing>(null, "Getting CMake Minimum Required Version", false) {
      override fun compute(indicator: ProgressIndicator) = cMakeVersionExtractor()
    })
  }
}

/**
 * [cMakeProjectName] - name of the CMake project that will be specified.
 * [cppStandard] - standard of language that will be specified in CMake.
 * [gtestVersion] - 'google test' framework version that will be loaded.
 *
 * NOTE: if some parameter is `null` it will be ignored and not be added to result map.
 * Use `null` value for the parameter only if it isn't used in the template.
 */
fun getCMakeTemplateVariables(
  cMakeProjectName: String? = null,
  cppStandard: String? = null,
  gtestVersion: String? = null
): Map<String, Any> {
  val result = mutableMapOf(CMAKE_MINIMUM_REQUIRED_LINE to cMakeMinimumRequired)

  cMakeProjectName?.let { result[EduNames.PROJECT_NAME] = it }
  cppStandard?.let { result[CPP_STANDARD_LINE] = it }
  gtestVersion?.let { result[GTEST_VERSION] = it }

  return result
}

fun addCMakeList(task: Task, projectName: String, cppStandard: String): TaskFile {
  val text = GeneratorUtils.getInternalTemplateText(getCppParameters(task.course).taskCMakeList,
                                                    getCMakeTemplateVariables(projectName, cppStandard))

  val taskFile = TaskFile(CMakeListsFileType.FILE_NAME, text)
  taskFile.isVisible = false

  task.addTaskFile(taskFile)

  return taskFile
}

fun getCMakeProjectUniqueName(task: Task, nameExtractor: (StudyItem) -> String = ::getDefaultName): String {
  val lesson = task.lesson
  val section = lesson.section

  val sectionPart = section?.let { nameExtractor(it) } ?: "global"
  val lessonPart = nameExtractor(lesson)
  val taskPart = nameExtractor(task)

  return "$sectionPart-$lessonPart-$taskPart"
}

fun getDefaultName(item: StudyItem) = when (item) {
  is Section -> "${EduNames.SECTION}${item.index}"
  is Lesson -> "${EduNames.LESSON}${item.index}"
  is Task -> "${EduNames.TASK}${item.index}"
  else -> "NonCommonStudyItem${item.index}"
}