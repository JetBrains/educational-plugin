package com.jetbrains.edu.learning.navigation

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting

@Service(Service.Level.PROJECT)
class StudyItemSelectionService(private val project: Project, private val scope: CoroutineScope) {
  private val studyItemSettings = MutableSharedFlow<NavigationProperties?>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

  init {
    scope.launch {
      studyItemSettings.collectLatest { currentStudyItem ->
        val course = project.course ?: return@collectLatest
        val studyItemId = currentStudyItem?.currentStudyItem ?: return@collectLatest
        if (studyItemId == -1) return@collectLatest

        course.allTasks.firstOrNull {
          it.id == studyItemId
          || it.lesson.id == studyItemId
          || it.lesson.section?.id == studyItemId
        }?.let {
          withContext(Dispatchers.EDT) {
            // If studyItemId corresponds to a task, we must navigate specifically to that task.
            // Otherwise, if studyItemId corresponds to a lesson or a section, we navigate to the first task of that study item,
            // and for framework lessons that means that we navigate to the current task of that lesson.
            val studyItemIsATask = it.id == studyItemId

            NavigationUtils.navigateToTask(project, it, forceSpecificTaskInFrameworkLesson = studyItemIsATask)
          }
        }
      }
    }
  }

  fun setCurrentStudyItem(currentStudyItem: Int) {
    scope.launch {
      studyItemSettings.emit(NavigationProperties(currentStudyItem))
    }
  }

  @VisibleForTesting
  fun lastSetCurrentStudyItem(): Int? = studyItemSettings.replayCache.lastOrNull()?.currentStudyItem

  companion object {
    fun getInstance(project: Project): StudyItemSelectionService = project.service()
  }
}

private data class NavigationProperties(val currentStudyItem: Int)