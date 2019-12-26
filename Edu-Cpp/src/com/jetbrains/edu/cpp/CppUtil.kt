package com.jetbrains.edu.cpp

import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.cmake.CMakeLanguage
import com.intellij.openapi.project.Project
import com.jetbrains.cidr.lang.OCLanguage
import com.jetbrains.cmake.CMakeListsFileType
import com.jetbrains.cmake.psi.CMakeCommand
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.CourseProjectGenerator

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
