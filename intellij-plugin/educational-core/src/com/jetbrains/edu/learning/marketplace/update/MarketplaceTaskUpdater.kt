package com.jetbrains.edu.learning.marketplace.update

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.update.MarketplaceItemUpdater
import com.jetbrains.edu.learning.update.TaskUpdater

class MarketplaceTaskUpdater(project: Project, lesson: Lesson) : TaskUpdater(project, lesson), MarketplaceItemUpdater<Task>