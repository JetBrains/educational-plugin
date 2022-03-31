package com.jetbrains.edu.cpp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cmake.CMakeLanguage
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.cmake.psi.CMakeCommand
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.getDefaultName
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator
import com.jetbrains.edu.learning.stepik.course.StepikCourse

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
  taskFile.isVisible = false

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

fun isEduCppProject(project: Project): Boolean {
  val course = StudyTaskManager.getInstance(project).course
  if (course != null) {
    // Checks that, created course is OCLanguage course
    return course.languageById == OCLanguage.getInstance()
  }

  // Checks that, course will be created with OCLanguage
  val baseDir = project.courseDir
  val modeToCreate = baseDir.getUserData(CourseProjectGenerator.COURSE_MODE_TO_CREATE)
  val languageId = baseDir.getUserData(CourseProjectGenerator.COURSE_LANGUAGE_ID_TO_CREATE)

  return modeToCreate != null && languageId == OCLanguage.getInstance().id
}

fun PsiFile.findCMakeCommand(commandName: String): CMakeCommand? {
  assert(language == CMakeLanguage.INSTANCE) { "Use this method only for CMake files!" }
  return PsiTreeUtil.findChildrenOfType(this, CMakeCommand::class.java)
    .firstOrNull { it.name.equals(commandName, true) }
}
