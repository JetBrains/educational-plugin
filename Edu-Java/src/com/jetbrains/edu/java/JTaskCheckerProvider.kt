package com.jetbrains.edu.java

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.OutputTaskChecker
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.checker.TheoryTaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.OutputTask
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask

class JTaskCheckerProvider : TaskCheckerProvider {
    override fun getEduTaskChecker(task: EduTask, project: Project) = JTaskChecker(task, project)
    override fun getOutputTaskChecker(task: OutputTask, project: Project) = OutputTaskChecker(task, project)
    override fun getTheoryTaskChecker(task: TheoryTask, project: Project) = TheoryTaskChecker(task, project)
}
