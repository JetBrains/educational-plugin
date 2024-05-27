package com.jetbrains.edu.learning.codeforces.update

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseUpdateListener
import com.jetbrains.edu.learning.codeforces.CodeforcesNames
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesCourse
import com.jetbrains.edu.learning.courseFormat.codeforces.CodeforcesTask
import com.jetbrains.edu.learning.courseFormat.codeforces.ContestParameters
import com.jetbrains.edu.learning.courseFormat.ext.getTaskTextFromTask
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.update.UpdateUtils.updateTaskDescription
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class CodeforcesCourseUpdater(private val project: Project, val course: CodeforcesCourse) {
  fun updateCourse(onFinish: (isUpdated: Boolean) -> Unit) {
    runInBackground(project, EduCoreBundle.message("update.check")) {
      val contestParameters = ContestParameters(
        id = course.id,
        languageId = course.languageId,
        locale = course.languageCode,
        endDateTime = course.endDateTime
      )
      val remoteContest = CodeforcesConnector.getInstance().getContest(contestParameters).onError {
        LOG.error(it)
        return@runInBackground
      }

      invokeAndWaitIfNeeded {
        if (project.isDisposed) return@invokeAndWaitIfNeeded

        val updatedTasks = updateContest(remoteContest) ?: return@invokeAndWaitIfNeeded
        val isUpdated = updatedTasks.isNotEmpty()

        if (isUpdated) {
          TaskToolWindowView.getInstance(project).updateTaskDescription()
          updatedTasks.forEach {
            @Suppress("DialogTitleCapitalization")
            EduNotificationManager.showInfoNotification(
              project,
              EduCoreBundle.message("update.notification.title"),
              EduCoreBundle.message("codeforces.task.description.was.updated.notification", it)
            )
          }
          project.messageBus.syncPublisher(CourseUpdateListener.COURSE_UPDATE).courseUpdated(project, course)
        }

        onFinish(isUpdated)
      }
    }
  }

  private fun updateContest(remoteCourse: CodeforcesCourse): List<String>? {
    val tasks = course.getTasks() ?: return null
    val remoteTasks = remoteCourse.getTasks() ?: return null
    val updatedTasks = mutableListOf<String>()

    for ((task, remoteTask) in tasks.zip(remoteTasks)) {
      if (!task.needToBeUpdated(project, remoteTask)) continue

      updateTaskDescription(project, task, remoteTask)
      YamlFormatSynchronizer.saveItem(task)
      updatedTasks.add(task.name)
    }

    val courseDir = project.courseDir
    for (additionalFile in remoteCourse.additionalFiles) {
      GeneratorUtils.createChildFile(project, courseDir, additionalFile.name, additionalFile.contents)
    }
    return updatedTasks
  }

  private fun CodeforcesTask.needToBeUpdated(project: Project, remoteTask: CodeforcesTask): Boolean {
    val descriptionText = runReadAction { this.getTaskTextFromTask(project) }
    // TODO think about tests
    return descriptionText != remoteTask.descriptionText
  }

  private fun CodeforcesCourse.getLesson(): Lesson? = getLesson(CodeforcesNames.CODEFORCES_PROBLEMS)

  private fun CodeforcesCourse.getTasks(): List<CodeforcesTask>? = getLesson()?.taskList?.filterIsInstance<CodeforcesTask>()

  companion object {
    private val LOG: Logger = Logger.getInstance(CodeforcesCourseUpdater::class.java)
  }
}