package com.jetbrains.edu.cpp

import com.intellij.ide.fileTemplates.FileTemplate
import com.intellij.ide.fileTemplates.FileTemplateManager
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.Task

fun generateDefaultName(item: StudyItem) = when (item) {
  is Section -> "${EduNames.SECTION}${item.index}"
  is Lesson -> "${EduNames.LESSON}${item.index}"
  is Task -> "${EduNames.TASK}${item.index}"
  else -> "NonCommonStudyItem${item.index}"
}

fun getTemplate(templateName: String): FileTemplate {
  return FileTemplateManager.getDefaultInstance().findInternalTemplate(templateName)
}

fun addCMakeList(task: Task, cppStandard: String?): TaskFile {
  val lesson = task.lesson
  val section = lesson.section
  val cMakeListFile = TaskFile()

  cMakeListFile.apply {
    name = CMakeListsFileType.FILE_NAME
    isVisible = false
    setText(generateCMakeListText(
      CppCourseBuilder.EDU_TASK_CMAKE_LIST_TEMP,
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
                     CppCourseBuilder.CMAKE_MINIMUM_REQUIRED_LINE to CppCourseBuilder.CMAKE_MINIMUM_REQUIRED,
                     CppCourseBuilder.CPP_STANDARD to cppStandard).filterValues { it != null }
  return templateName.getText(params)
}
