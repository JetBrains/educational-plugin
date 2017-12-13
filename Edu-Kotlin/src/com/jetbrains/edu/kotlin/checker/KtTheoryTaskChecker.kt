package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.gradle.GradleTheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class KtTheoryTaskChecker(task: TheoryTask, project: Project) : GradleTheoryTaskChecker(task, project) {
    override fun getMainClassName(project: Project): String? = kotlinMainClassName(project)
}
