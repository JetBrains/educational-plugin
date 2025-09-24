package com.jetbrains.edu.cpp

import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cmake.CMakeLanguage
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard
import com.jetbrains.cmake.psi.CMakeCommand
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.stepik.StepikCourse
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getDefaultName

/**
 * Create CMake for the task and add it as taskFile.
 *
 * @return created taskFile
 */
fun Task.addCMakeList(projectName: String, cppStandard: String = ""): TaskFile {
  val templateInfo = getCppTemplates(course).let {
    if (this is EduTask) it.testTaskCMakeList else it.executableTaskCMakeList
  }

  val taskFile = TaskFile(CMakeListsFileType.FILE_NAME, templateInfo.getText(projectName, cppStandard))
  if (course !is HyperskillCourse) {
    taskFile.isVisible = false
  }

  addTaskFile(taskFile)

  return taskFile
}

fun getCMakeProjectName(task: Task): String {
  val nameExtractor = if (task.course is StepikCourse) ::getDefaultName else StudyItem::name

  val lesson = task.lesson
  val section = lesson.section

  val sectionPart = section?.let { nameExtractor(it) } ?: "global"
  val lessonPart = nameExtractor(lesson)
  val taskPart = nameExtractor(task)

  return "${sectionPart.sanitized()}-${lessonPart.sanitized()}-${taskPart.sanitized()}"
}

private fun String.sanitized(): String = FileUtil.sanitizeFileName(this, true)

fun PsiFile.findCMakeCommand(commandName: String): CMakeCommand? {
  assert(language == CMakeLanguage.INSTANCE) { "Use this method only for CMake files!" }
  return PsiTreeUtil.findChildrenOfType(this, CMakeCommand::class.java)
    .firstOrNull { it.name.equals(commandName, true) }
}

fun getLanguageVersions(): List<String> = CMakeRecognizedCPPLanguageStandard.entries.map { it.standard }
