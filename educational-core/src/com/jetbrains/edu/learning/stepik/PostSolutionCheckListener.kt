package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.checker.CheckListener
import com.jetbrains.edu.learning.checker.CheckResult
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task

class PostSolutionCheckListener : CheckListener {
    override fun afterCheck(project: Project, task: Task, result: CheckResult) {
        val isLoggedIn = EduSettings.getInstance().user != null
        val isStudyCourse = task.lesson.course.isStudy
        if (isLoggedIn && isStudyCourse && task.isToSubmitToStepik) {
            StepikConnector.postSolution(task, task.status == CheckStatus.Solved, project)
        }
    }
}