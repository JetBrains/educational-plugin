package com.jetbrains.edu.aiHints.python

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.testFramework.utils.vfs.getPsiFile
import com.intellij.util.concurrency.annotations.RequiresReadLock
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.findTask

internal const val PY_LESSON: String = "py_lesson"
internal const val PY_TASK: String = "py_task"
internal const val PY_TASK_FILE: String = "py_task.py"

@RequiresReadLock
internal fun getPsiFile(project: Project, lessonName: String, taskName: String, fileName: String): PsiFile {
  val course = StudyTaskManager.getInstance(project).course ?: error("Course is null")
  val taskFile = course.findTask(lessonName, taskName).getTaskFile(fileName) ?: error("TaskFile $fileName is not found")
  val virtualFile = taskFile.getVirtualFile(project) ?: error("VirtualFile for $fileName is not found")
  return virtualFile.getPsiFile(project)
}