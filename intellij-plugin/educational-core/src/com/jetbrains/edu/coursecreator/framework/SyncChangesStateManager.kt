package com.jetbrains.edu.coursecreator.framework

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.edu.coursecreator.CCUtils
import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.isFeatureEnabled
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class SyncChangesStateManager(private val project: Project) {
  private val stateStorage = ConcurrentHashMap<TaskFile, SyncChangesTaskFileState>()

  fun getSyncChangesState(taskFile: TaskFile): SyncChangesTaskFileState? {
    if (!checkRequirements(taskFile.task.lesson)) return null
    return stateStorage[taskFile]
  }

  private fun checkRequirements(lesson: Lesson): Boolean {
    return CCUtils.isCourseCreator(project) && lesson is FrameworkLesson && isFeatureEnabled(EduExperimentalFeatures.CC_FL_SYNC_CHANGES)
  }

  companion object {
    fun getInstance(project: Project): SyncChangesStateManager = project.service()
  }
}
