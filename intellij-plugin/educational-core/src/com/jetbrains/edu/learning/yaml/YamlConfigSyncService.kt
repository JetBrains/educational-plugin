package com.jetbrains.edu.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseFormat.StudyItem

/**
 * Manages persistence of YAML configuration files.
 * Ensures that all save requests are executed sequentially.
 * On service shutdown, all pending saves continue running.
 */
interface YamlConfigSyncService {
  /**
   * Request to save YAML configuration for a study item. The save is run asynchronously.
   * When possible, prefer the [save] method, it suspends until the save is complete.
   */
  fun requestSave(studyItem: StudyItem, configName: String, mapper: ObjectMapper)

  /**
   * Request to save YAML configuration for a study item.
   * Suspends until the save is complete. After this method returns, the file is saved.
   */
  suspend fun save(studyItem: StudyItem, configName: String, mapper: ObjectMapper)

  /**
   * Executes the action avoiding saving YAML configuration file [configFile].
   */
  fun <T> withSaveSuppressed(configFile: VirtualFile?, action: () -> T): T

  companion object {
    fun getInstance(project: Project): YamlConfigSyncService = project.service()
  }
}