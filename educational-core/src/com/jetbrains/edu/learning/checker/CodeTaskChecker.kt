package com.jetbrains.edu.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.tasks.CodeTask

class CodeTaskChecker(task: CodeTask, project: Project) : TaskChecker<CodeTask>(task, project) {
    override fun onTaskFailed(message: String, details: String?) {
        super.onTaskFailed("Wrong solution", details)
        CheckUtils.showTestResultsToolWindow(project, message)
    }
}
