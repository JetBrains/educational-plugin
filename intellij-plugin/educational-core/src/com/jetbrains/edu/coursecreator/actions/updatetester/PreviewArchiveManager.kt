package com.jetbrains.edu.coursecreator.actions.updatetester

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class PreviewArchiveManager {

  var previewLoadedFrom: String? = null
  var lastPreviewCreatedAt: String? = null

  companion object {
    fun getInstance(project: Project): PreviewArchiveManager = project.service()
  }
}