package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.gradle.GradleOutputTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask

class KtOutputTaskChecker(task: OutputTask, project: Project) : GradleOutputTaskChecker(task, project) {
    override fun getMainClassName(project: Project): String? = kotlinMainClassName(project)
}
