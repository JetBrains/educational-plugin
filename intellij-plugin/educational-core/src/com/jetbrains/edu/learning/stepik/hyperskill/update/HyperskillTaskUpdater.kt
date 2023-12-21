package com.jetbrains.edu.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.update.HyperskillItemUpdater
import com.jetbrains.edu.learning.update.TaskUpdater

class HyperskillTaskUpdater(project: Project, lesson: Lesson) : TaskUpdater(project, lesson), HyperskillItemUpdater<Task>