package com.jetbrains.edu.cpp

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.cmake.psi.CMakeCommand
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Create CMake for the task and add it as taskFile.
 *
 * @return created taskFile
 */
fun Task.addCMakeList(projectName: String, cppStandard: String): TaskFile {
  val templateInfo = getCppTemplates(course).taskCMakeList

  val taskFile = TaskFile(CMakeListsFileType.FILE_NAME, templateInfo.getText(projectName, cppStandard))
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

fun PsiFile.findCMakeCommand(commandName: String): CMakeCommand? =
  PsiTreeUtil.findChildrenOfType(this, CMakeCommand::class.java)
    .firstOrNull { it.name.equals(commandName, true) }
