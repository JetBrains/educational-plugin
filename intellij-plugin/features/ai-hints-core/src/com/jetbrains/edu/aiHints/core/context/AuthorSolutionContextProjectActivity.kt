package com.jetbrains.edu.aiHints.core.context

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.asSafely
import com.jetbrains.edu.aiHints.core.EduAIHintsProcessor
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask

class AuthorSolutionContextProjectActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    val course = project.course?.asSafely<EduCourse>() ?: return
    // AI Hints are available only for Marketplace courses in the Student mode, and for a subset of programming languages only
    if (!course.isStudy || !course.isMarketplace || EduAIHintsProcessor.forCourse(course) == null) return
    val eduTasks = course.allTasks.filterIsInstance<EduTask>() // AI Hints available only for EduTasks
    for (task in eduTasks) {
      task.authorSolutionContext = AuthorSolutionContext.create(project, task)
    }
  }
}