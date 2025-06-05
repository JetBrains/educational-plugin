package com.jetbrains.edu.learning.navigation

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class StudyItemSelectionService(private val project: Project, scope: CoroutineScope) {
  private val _studyItemSettings = MutableStateFlow<NavigationProperties?>(null)
  val studyItemSettings = _studyItemSettings.asStateFlow()

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
            NavigationUtils.navigateToTask(project, it)
          }
        }
      }
    }
  }

  fun setCurrentStudyItem(currentStudyItem: Int) {
    _studyItemSettings.value = NavigationProperties(currentStudyItem)
  }

  companion object {
    fun getInstance(project: Project): StudyItemSelectionService = project.service()
  }
}

data class NavigationProperties(val currentStudyItem: Int)