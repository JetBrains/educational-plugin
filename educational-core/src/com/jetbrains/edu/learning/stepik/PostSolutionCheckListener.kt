package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.api.Submission
import com.jetbrains.edu.learning.taskDescription.ui.TaskDescriptionView

class PostSolutionCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    val course = task.lesson.course
    if (course is EduCourse && course.isRemote && EduSettings.isLoggedIn() && course.isStudy) {
      if (task.isToSubmitToStepik) {
        ApplicationManager.getApplication().executeOnPooledThread {
          val submission: Submission? = StepikSolutionsLoader.postSolution(task, task.status == CheckStatus.Solved, project)
          submission?.status = if (task.status == CheckStatus.Solved) EduNames.CORRECT else EduNames.WRONG
          SubmissionsManager.addToSubmissions(task.id, submission)
          runInEdt { TaskDescriptionView.getInstance(project).updateAdditionalTaskTab() }
        }
      }
    }
  }
}