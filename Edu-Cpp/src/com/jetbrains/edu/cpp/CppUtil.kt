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

fun getDefaultName(item: StudyItem) = when (item) {
  is Section -> "${EduNames.SECTION}${item.index}"
  is Lesson -> "${EduNames.LESSON}${item.index}"
  is Task -> "${EduNames.TASK}${item.index}"
  else -> "NonCommonStudyItem${item.index}"
}

fun addCMakeList(task: Task, cppStandard: String): TaskFile {
  val text = GeneratorUtils.getInternalTemplateText(getParametersByCourse(task.course).taskCMakeList,
                                                    getCMakeTemplateVariables(getCMakeProjectUniqueName(task), cppStandard))

  val taskFile = TaskFile(CMakeListsFileType.FILE_NAME, text)
  taskFile.isVisible = false

  task.addTaskFile(taskFile)

  return taskFile
}

private fun getCMakeProjectUniqueName(task: Task): String {
  val lesson = task.lesson
  val section = lesson.section

  val sectionPart = section?.let { getDefaultName(it) } ?: "global"
  val lessonPart = getDefaultName(lesson)
  val taskPart = getDefaultName(task)

  return "$sectionPart-$lessonPart-$taskPart"
}

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
 *  [cMakeProjectName] — name of the CMake project that will be specified there
 *  [cppStandard] — if not null, used as version of language that will be specified in CMake, else will be omitted at all.
 *    Don't specify this parameter if only it doesn't used in template.
 */
fun getCMakeTemplateVariables(cMakeProjectName: String, cppStandard: String? = null): Map<String, Any> {
  val values = mutableMapOf(CMAKE_MINIMUM_REQUIRED_LINE to cMakeMinimumRequired,
                            EduNames.PROJECT_NAME to cMakeProjectName)
  if (cppStandard != null) {
    values[CPP_STANDARD_LINE] = cppStandard
  }

  return values
}
