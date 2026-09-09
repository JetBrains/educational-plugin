package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.StudyItem

interface YamlConfigSyncService {
  fun save(studyItem: StudyItem, configName: String, mapper: ObjectMapper)

  suspend fun saveSync(studyItem: StudyItem, configName: String, mapper: ObjectMapper)

  fun <T> withSaveSuppressed(configFile: VirtualFile?, action: () -> T): T

  companion object {
    fun getInstance(project: Project): YamlConfigSyncService = project.service()
  }
}