package com.jetbrains.edu.kotlin

import com.intellij.openapi.project.Project
import com.jetbrains.edu.kotlin.check.KtOutputTaskChecker
import com.jetbrains.edu.kotlin.check.KtTaskChecker
import com.jetbrains.edu.kotlin.check.KtTheoryTaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class KTaskCheckerProvider : TaskCheckerProvider {
    override fun getEduTaskChecker(task: EduTask, project: Project) = KtTaskChecker(task, project)
    override fun getOutputTaskChecker(task: OutputTask, project: Project) = KtOutputTaskChecker(task, project)
    override fun getTheoryTaskChecker(task: TheoryTask, project: Project) = KtTheoryTaskChecker(task, project)
}