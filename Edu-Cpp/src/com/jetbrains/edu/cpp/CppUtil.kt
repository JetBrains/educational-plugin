package com.jetbrains.edu.cpp

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.*
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

private val cMakeMinimumRequired: String by lazy {
  val cMakeVersionExtractor = {
    CLionProjectWizardUtils.getCMakeMinimumRequiredLine(CMake.readCMakeVersion(CPPToolchains.getInstance().defaultToolchain))
  }

  val progressManager = ProgressManager.getInstance()

  if (progressManager.hasProgressIndicator()) {
    progressManager.runProcess<String>({ cMakeVersionExtractor() }, null)
  }
  else {
    progressManager.run(object : WithResult<String, Nothing>(null, "Getting CMake Minimum Required Version", false) {
      override fun compute(indicator: ProgressIndicator) = cMakeVersionExtractor()
    })
  }
}

/**
 * By default, adds only CMake minimum required line.
 * When some parameter is not null it will be used how parameter, otherwise parameter will be ignored at all.
 * Omits parameters if only they aren't used in the template!
 */
fun getCMakeTemplateVariables(cMakeProjectName: String? = null, cppStandard: String? = null): Map<String, Any> {
  val values = mutableMapOf(CMAKE_MINIMUM_REQUIRED_LINE to cMakeMinimumRequired)

  if (cMakeProjectName != null) values[EduNames.PROJECT_NAME] = cMakeProjectName
  if (cppStandard != null) values[CPP_STANDARD_LINE] = cppStandard

  return values
}

fun addCMakeList(task: Task, cppStandard: String): TaskFile {
  val text = GeneratorUtils.getInternalTemplateText(getCppParameters(task.course).taskCMakeList,
                                                    getCMakeTemplateVariables(getCMakeProjectUniqueName(task), cppStandard))

  val taskFile = TaskFile(CMakeListsFileType.FILE_NAME, text)
  taskFile.isVisible = false

  task.addTaskFile(taskFile)

  return taskFile
}

fun getCMakeProjectUniqueName(task: Task): String {
  val lesson = task.lesson
  val section = lesson.section

  val sectionPart = section?.let { getDefaultName(it) } ?: "global"
  val lessonPart = getDefaultName(lesson)
  val taskPart = getDefaultName(task)

  return "$sectionPart-$lessonPart-$taskPart"
}

fun getDefaultName(item: StudyItem) = when (item) {
  is Section -> "${EduNames.SECTION}${item.index}"
  is Lesson -> "${EduNames.LESSON}${item.index}"
  is Task -> "${EduNames.TASK}${item.index}"
  else -> "NonCommonStudyItem${item.index}"
}