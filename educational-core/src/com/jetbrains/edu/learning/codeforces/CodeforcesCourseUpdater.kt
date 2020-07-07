package com.jetbrains.edu.learning.codeforces

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.CourseUpdater
import com.jetbrains.edu.learning.EduUtils.getTaskTextFromTask
import com.jetbrains.edu.learning.codeforces.api.CodeforcesConnector
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesCourse
import com.jetbrains.edu.learning.codeforces.courseFormat.CodeforcesTask
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.onError
import com.jetbrains.edu.learning.runInBackground
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class CodeforcesCourseUpdater(project: Project, course: CodeforcesCourse) : CourseUpdater<CodeforcesCourse>(project, course) {
  private val LOG: Logger = Logger.getInstance(CodeforcesCourseUpdater::class.java)
  private val updatedTasks: MutableSet<String> = mutableSetOf()

  fun updateCourseAndDoActions(onFinish: () -> Unit = {},
                               onNothingUpdated: () -> Unit = {}) {
    updateCourse {
      if (updatedTasks.isNotEmpty()) {
        updateTaskDescription()
      }
      else {
        onNothingUpdated()
      }
      onFinish()
    }
  }

  override fun updateCourse(onFinish: () -> Unit) {
    runInBackground(project, EduCoreBundle.message("update.check")) {
      if (project.isDisposed) return@runInBackground
      val contestParameters = ContestParameters(id = course.id,
                                                languageId = course.language,
                                                locale = course.languageCode,
                                                endDateTime = course.endDateTime)
      val remoteContest = CodeforcesConnector.getInstance().getContest(contestParameters).onError {
        LOG.error(it)
        return@runInBackground
      }

      doUpdate(remoteContest)
      onFinish()
    }
  }

  override fun doUpdate(remoteCourse: CodeforcesCourse?) {
    updateProjectLesson(remoteCourse)
    updatedTasks.forEach {
      showUpdateCompletedNotification(EduCoreBundle.message("codeforces.task.description.was.updated.notification", it))
    }
  }

  @VisibleForTesting
  override fun updateProjectLesson(remoteCourse: CodeforcesCourse?): Boolean {
    val tasks = course.getTasks() ?: return true
    val remoteTasks = remoteCourse?.getTasks() ?: return true

    invokeAndWaitIfNeeded {
      if (project.isDisposed) return@invokeAndWaitIfNeeded

      for ((task, remoteTask) in tasks.zip(remoteTasks)) {
        if (!task.needToBeUpdated(project, remoteTask)) continue

        updatedTasks.add(task.name)
        updateTaskDescription(task, remoteTask)
        YamlFormatSynchronizer.saveItem(task)
      }

      val courseDir = project.courseDir
      for (additionalFile in remoteCourse.additionalFiles) {
        GeneratorUtils.createChildFile(courseDir, additionalFile.name, additionalFile.text)
      }
    }
    return false
  }

  private fun updateTaskDescription() {
    ApplicationManager.getApplication().invokeLater {
      TaskDescriptionView.getInstance(project).updateTaskDescription()
    }
  }

  private fun CodeforcesTask.needToBeUpdated(project: Project, remoteTask: CodeforcesTask): Boolean {
    val descriptionText = runReadAction { getTaskTextFromTask(project, this) }
    // TODO think about tests
    return descriptionText != remoteTask.descriptionText
  }

  private fun CodeforcesCourse.getLesson(): Lesson? = getLesson(CodeforcesNames.CODEFORCES_PROBLEMS)

  private fun CodeforcesCourse.getTasks(): List<CodeforcesTask>? {
    val lesson = getLesson() ?: return null
    return lesson.taskList.map {
      if (it !is CodeforcesTask) return null
      @Suppress("USELESS_CAST")
      it as CodeforcesTask
    }
  }
}