package com.jetbrains.edu.cpp

import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
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
import com.jetbrains.edu.learning.stepik.course.StepikCourse

fun generateDefaultName(item: StudyItem) = when (item) {
  is Section -> "${EduNames.SECTION}${item.index}"
  is Lesson -> "${EduNames.LESSON}${item.index}"
  is Task -> "${EduNames.TASK}${item.index}"
  else -> "NonCommonStudyItem${item.index}"
}

fun addCMakeList(task: Task, cppStandard: String?): TaskFile {
  val lesson = task.lesson
  val section = lesson.section
  val cMakeListFile = TaskFile()

  cMakeListFile.apply {
    name = CMakeListsFileType.FILE_NAME
    isVisible = false
    setText(generateCMakeListText(
      when (task.course) {
        is StepikCourse -> TemplateManager.stepikTaskCMakeList
        else -> TemplateManager.eduTaskCMakeList
      },
      generateCMakeProjectUniqueName(section, lesson, task),
      cppStandard
    ))
  }
  task.addTaskFile(cMakeListFile)

  return cMakeListFile
}

private fun generateCMakeProjectUniqueName(section: Section?, lesson: Lesson, task: Task): String {
  val sectionPart = section?.let { generateDefaultName(it) } ?: "global"
  val lessonPart = generateDefaultName(lesson)
  val taskPart = generateDefaultName(task)

  return "$sectionPart-$lessonPart-$taskPart"
}

fun generateCMakeListText(templateName: FileTemplate, cppProjectName: String, cppStandard: String? = null): String {
  val params = mapOf(EduNames.PROJECT_NAME to cppProjectName,
                     TemplateManager.CMAKE_MINIMUM_REQUIRED_LINE to TemplateManager.cMakeMinimumRequired,
                     TemplateManager.CPP_STANDARD to cppStandard).filterValues { it != null }
  return templateName.getText(params)
}

object TemplateManager {
  private fun getTemplate(templateName: String): FileTemplate {
    return FileTemplateManager.getDefaultInstance().findInternalTemplate(templateName)
  }

  private const val EDU_MAIN_CMAKE_LIST = "EduMainCMakeList.txt"
  private const val STEPIK_MAIN_CMAKE_LIST = "StepikMainCMakeList.txt"

  val eduMainCMakeList: FileTemplate by lazy {
    getTemplate(EDU_MAIN_CMAKE_LIST)
  }

  val stepikMainCMakeList: FileTemplate by lazy {
    getTemplate(STEPIK_MAIN_CMAKE_LIST)
  }

  private const val EDU_TASK_CMAKE_LIST = "EduTaskCMakeList.txt"
  private const val STEPIK_TASK_CMAKE_LIST = "StepikTaskCMakeList.txt"

  val eduTaskCMakeList: FileTemplate by lazy {
    getTemplate(EDU_TASK_CMAKE_LIST)
  }

  val stepikTaskCMakeList: FileTemplate by lazy {
    getTemplate(STEPIK_TASK_CMAKE_LIST)
  }

  const val CMAKE_MINIMUM_REQUIRED_LINE = "CMAKE_MINIMUM_REQUIRED_LINE"
  const val CPP_STANDARD = "CPP_STANDARD"

  val cMakeMinimumRequired: String by lazy {
    CLionProjectWizardUtils.getCMakeMinimumRequiredLine(CMake.readCMakeVersion(CPPToolchains.getInstance().defaultToolchain))
  }

  fun initCMakeMinimumRequired() {
    cMakeMinimumRequired
  }
}