package com.jetbrains.edu.cpp

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.WithResult
import com.jetbrains.cidr.cpp.cmake.projectWizard.CLionProjectWizardUtils
import com.jetbrains.cidr.cpp.toolchains.CMake
import com.jetbrains.cidr.cpp.toolchains.CPPToolchains
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task

val cMakeMinimumRequired: String by lazy {
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
 * Create CMake for the task and add it as taskFile.
 *
 * @return created taskFile
 */
fun Task.addCMakeList(projectName: String, cppStandard: String): TaskFile {
  val templateInfo = getCppTemplates(course).taskCMakeList

  val taskFile = TaskFile(CMakeListsFileType.FILE_NAME,
                          templateInfo.getText { key ->
                            when (key) {
                              CppTemplates.CMAKE_MINIMUM_REQUIRED_LINE_KAY -> cMakeMinimumRequired
                              CppTemplates.PROJECT_NAME_KEY -> projectName
                              CppTemplates.CPP_STANDARD_LINE_KEY -> cppStandard
                              else -> ""
                            }
                          })
  taskFile.isVisible = false

  addTaskFile(taskFile)

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
  is FrameworkLesson -> "${EduNames.FRAMEWORK_LESSON}${item.index}"
  is Lesson -> "${EduNames.LESSON}${item.index}"
  is Task -> "${EduNames.TASK}${item.index}"
  else -> "NonCommonStudyItem${item.index}"
}