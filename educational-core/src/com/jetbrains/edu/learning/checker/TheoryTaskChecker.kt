package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class TheoryTaskChecker : TaskChecker() {
    override fun isAccepted(task: Task) = task is TheoryTask

    override fun onTaskSolved(task: Task, project: Project, message: String) {}

    override fun check(task: Task, project: Project) = CheckResult(CheckStatus.Solved, "")

    override fun checkOnRemote(task: Task, project: Project) = check(task, project)
}
