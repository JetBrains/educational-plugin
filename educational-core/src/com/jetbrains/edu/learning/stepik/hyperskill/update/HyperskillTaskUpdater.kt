package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.UnsupportedTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.stepik.hyperskill.courseFormat.RemoteEduTask
import com.jetbrains.edu.learning.update.EduCourseUpdater.TaskUpdate
import com.jetbrains.edu.learning.update.UpdateUtils
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

@Suppress("DuplicatedCode")
class HyperskillTaskUpdater(private val project: Project) {
  fun doUpdate(update: TaskUpdate) {
    return doUpdate(update.localTask, update.taskFromServer)
  }

  private fun doUpdate(localTask: Task, taskFromServer: Task) {
    if (!localTask.updateDate.before(taskFromServer.updateDate)) return
    val hasLocalTaskBecomeSupported = localTask is UnsupportedTask && taskFromServer !is UnsupportedTask
    if (hasLocalTaskBecomeSupported) {
      replaceTaskInCourse(localTask, taskFromServer)
    }
    if (localTask.status != CheckStatus.Solved || hasLocalTaskBecomeSupported) {
      // if name of remote task changes name of dir local task will not
      GeneratorUtils.createTaskContent(project, taskFromServer, localTask.getDir(project.courseDir)!!)
    }
    val frameworkLesson = localTask.lesson as? FrameworkLesson
    if (frameworkLesson != null && localTask.status != CheckStatus.Solved) {
      // With current logic of next/prev action for hyperskill tasks
      // update of non-test files makes sense only for first task
      UpdateUtils.updateFrameworkLessonFiles(project, frameworkLesson, localTask, taskFromServer, localTask.index == 1)
    }
    UpdateUtils.updateTaskDescription(project, localTask, taskFromServer)
    if (localTask is RemoteEduTask && taskFromServer is RemoteEduTask) {
      localTask.checkProfile = taskFromServer.checkProfile
    }
    YamlFormatSynchronizer.saveItemWithRemoteInfo(localTask)
  }

  private fun replaceTaskInCourse(localTask: Task, remoteTask: Task) {
    val lesson = localTask.parent
    lesson.removeItem(localTask)
    lesson.addItem(localTask.index - 1, remoteTask)
    remoteTask.index = localTask.index
    remoteTask.name = localTask.name
  }
}