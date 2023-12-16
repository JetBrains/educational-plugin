package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.submissions.isSignificantlyAfter
import com.jetbrains.edu.learning.update.TaskUpdater

class HyperskillTaskUpdater(project: Project, lesson: Lesson) : TaskUpdater(project, lesson) {

  override fun isLocalTaskOutdated(localTask: Task, remoteTask: Task): Boolean =
    remoteTask.updateDate.isSignificantlyAfter(localTask.updateDate)
}