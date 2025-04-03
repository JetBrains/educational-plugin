package com.jetbrains.edu.coursecreator.actions.updatetester

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.marketplace.update.MarketplaceCourseUpdaterNew
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.nio.file.Path

@Service(Service.Level.PROJECT)
class PreviewArchiveManager(
  private val project: Project,
  private val coroutineScope: CoroutineScope
) {

  var previewLoadedFrom: Path? = null
  var sourceProjectBasePath: String? = null

  fun findSourceProject(): Project? = ProjectManager.getInstance().openProjects.find {
    it.basePath == sourceProjectBasePath
  }

  fun updateFromPreview(remoteCourse: EduCourse) {
    val localCourse = project.course as? EduCourse ?: error("Preview could be created only for Edu courses")

    coroutineScope.launch {
      withContext(Dispatchers.IO) {
          MarketplaceCourseUpdaterNew(project, localCourse).update(remoteCourse)
      }
    }
  }

  companion object {
    fun getInstance(project: Project): PreviewArchiveManager = project.service()
  }
}