package com.jetbrains.edu.java

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.checker.TaskCheckerProvider
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class JTaskCheckerProvider : TaskCheckerProvider {
    override fun getEduTaskChecker(task: EduTask, project: Project) = JTaskChecker(task, project)
}
