package com.jetbrains.edu.learning.navigation

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NavigationProjectActivity(private val scope: CoroutineScope): ProjectActivity {
  override suspend fun execute(project: Project) {
    scope.launch {
      NavigationSettings.getInstance(project).navigationSettings.collectLatest { currentStudyItem ->
        val course = project.course ?: return@collectLatest
        val studyItemId = currentStudyItem?.currentStudyItem ?: return@collectLatest
        if (studyItemId == -1) return@collectLatest

        course.allTasks.firstOrNull {
          it.id == studyItemId
          || it.lesson.id == studyItemId
          || it.lesson.section?.id == studyItemId
        }?.let {
          invokeLater {
            NavigationUtils.navigateToTask(project, it)
          }
        }
      }
    }
  }
}