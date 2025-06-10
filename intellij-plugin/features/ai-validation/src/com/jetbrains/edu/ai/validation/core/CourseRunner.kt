package com.jetbrains.edu.ai.validation.core

import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.actionSystem.impl.SimpleDataContext
import com.intellij.openapi.application.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.waitForSmartMode
import com.intellij.psi.PsiDocumentManager
import com.jetbrains.edu.ai.validation.core.listener.UserCheckListener
import com.jetbrains.edu.ai.validation.core.model.StudentSolutionRecord
import com.jetbrains.edu.ai.validation.core.model.UserResult
import com.jetbrains.edu.ai.validation.core.service.UserResultsService
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.actions.CheckAction
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.ext.configurator
import com.jetbrains.edu.learning.courseFormat.ext.getDocument
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.framework.FrameworkLessonManager
import com.jetbrains.edu.learning.navigation.NavigationUtils
import com.jetbrains.edu.learning.ui.getUICheckLabel
import kotlinx.coroutines.*

class CourseRunner(
  private val project: Project,
  private val course: Course,
  private val solutions: List<StudentSolutionRecord>
) {

  fun collectUserResultInfo(onResultsCollected: (List<UserResult>) -> Unit) {
    CoroutineScope(Dispatchers.Default).launch {
      for ((lessonId, lesson) in course.lessons.withIndex()) {
        navigateToTask(lesson.taskList[0])
        for ((taskId, task) in lesson.taskList.withIndex()) {
          if (task !is EduTask) continue
          navigateToTask(task)
          progressTask(taskId, lessonId, lesson, task)
        }
      }
      onResultsCollected(UserResultsService.getInstance(project).getResults())
    }
  }

  private suspend fun progressTask(taskId: Int, lessonId: Int, lesson: Lesson, task: Task) {
    solutions.filter { it.lessonId == lessonId + 1 && it.taskId == taskId + 1 }.map { solution ->
      invokeAndWaitIfNeeded {
        replaceContent(lesson, task, solution.code)
      }
      runCheckAction(task)
    }
  }

  private fun navigateToTask(task: Task) = invokeAndWaitIfNeeded {
    NavigationUtils.navigateToTask(project, task, fromTask = project.eduState?.task, showDialogIfConflict = false)
  }

  private fun replaceContent(lesson: Lesson, task: Task, newCode: String) {
    assert(project.eduState?.task == task)
    val state = project.eduState ?: return
    replaceDocumentText(state.taskFile, project, newCode)
    if (!CCUtils.isCourseCreator(project) && lesson is FrameworkLesson) {
      changeStateForMainFile(task, newCode)
    }
  }

  private fun changeStateForMainFile(task: Task, newCode: String) {
    val taskFileName = course.configurator?.courseBuilder?.taskTemplateName(course) ?: "Task"
    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
    val externalState = task.taskFiles.mapValues {
      if (taskFileName in it.key) {
        newCode
      } else {
        it.value.contents.textualRepresentation
      }
    }
    frameworkLessonManager.saveExternalChanges(task, externalState)
  }

  private fun replaceDocumentText(taskFile: TaskFile, project: Project, solution: String) {
    val currentDocument = taskFile.getDocument(project)
    runWriteAction { currentDocument?.setText(solution) }
    currentDocument?.let { PsiDocumentManager.getInstance(project).commitDocument(it) }
  }

  private suspend fun runCheckAction(task: Task) {
    project.waitForSmartMode()
    val checkListener = CheckListener.EP_NAME.findExtension(UserCheckListener::class.java) ?: error("Check listener not found")
    checkListener.clear()
    executeCheckAction(task)
    checkListener.wait()
  }

  private fun executeCheckAction(task: Task) = runInEdt {
    val dataContext = SimpleDataContext.getProjectContext(project)
    val action = CheckAction(task.getUICheckLabel())
    val event = AnActionEvent.createEvent(
      action, dataContext,
      action.getTemplatePresentation().clone(), "", ActionUiKind.NONE, null
    )
    ActionUtil.invokeAction(action, event, null)
  }
}
