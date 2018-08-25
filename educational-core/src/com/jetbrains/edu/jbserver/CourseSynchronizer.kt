package com.jetbrains.edu.jbserver

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.jetbrains.edu.coursecreator.stepik.StepikCourseChangeHandler
import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task


fun StudyItem.isChanged()
  = stepikChangeStatus != StepikChangeStatus.UP_TO_DATE


class CourseSynchronizer(val course: Course, val project: Project) {

  init { sync(course) }

  private fun sync(studyItem: StudyItem): Unit = when (studyItem) {
    is ItemContainer -> {
      studyItem.items.forEach { sync(it) }
      if (studyItem.items.filter { it.isChanged() }.isNotEmpty())
      {
        StepikCourseChangeHandler.contentChanged(studyItem)
      } else {
        StepikCourseChangeHandler.contentUnchanged(studyItem)
      }
    }
    is Lesson -> {
      val changedTasks = studyItem.taskList.filter { it.isChanged() }
      if (changedTasks.isNotEmpty()) {
        StepikCourseChangeHandler.contentChanged(studyItem)
        changedTasks.forEach { syncTask(it) }
      } else {
        StepikCourseChangeHandler.contentUnchanged(studyItem)
      }
    }
    else -> {}
  }

  private fun syncTask(task: Task) {
    val taskDir = task.getTaskDir(project) ?: return
    // Update task files
    val newTaskFiles = mutableMapOf<String, TaskFile>()
    task.getTaskFiles().forEach {
      it.value.name = it.key // update name if renamed
      val answerFile = EduUtils.findTaskFileInDir(it.value, taskDir) ?: return
      val taskFile = EduUtils.createStudentFile(project, answerFile, task) ?: return
      newTaskFiles[taskFile.name] = taskFile
    }
    task.taskFiles.clear()
    task.taskFiles.putAll(newTaskFiles)
    // Update test files
    val newTestFiles = mutableMapOf<String, String>()
    EduUtils.getTestFiles(task, project).forEach {
      newTestFiles[it.name] = VfsUtilCore.loadText(it)
    }
    task.testsText.clear()
    task.testsText.putAll(newTestFiles)
  }

}
