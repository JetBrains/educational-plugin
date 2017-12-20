package com.jetbrains.edu.kotlin.checker

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checker.gradle.GradleEduTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class KTaskCheckerProvider : TaskCheckerProvider {
    override fun getEduTaskChecker(task: EduTask, project: Project) = GradleEduTaskChecker(task, project)
    override fun getOutputTaskChecker(task: OutputTask, project: Project) = KtOutputTaskChecker(task, project)
    override fun getTheoryTaskChecker(task: TheoryTask, project: Project) = KtTheoryTaskChecker(task, project)
}
