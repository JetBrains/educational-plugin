package com.jetbrains.edu.learning.update

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware

@Service(Service.Level.PROJECT)
@State(name = "EduUpdateHistory", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class UpdateHistoryService : SerializablePersistentStateComponent<UpdateHistoryService.State>(State(emptyList())), EduTestAware {

  data class State(var updates: List<UpdateItem> = mutableListOf())

  fun updateHappened(updateItem: UpdateItem) {
    val existingUpdates = state.updates
    state = State(existingUpdates + updateItem)
  }

  fun updatesString(): String = state.updates.joinToString(separator = ",")

  override fun cleanUpState() {
    state = State(emptyList())
  }

  companion object {
    fun getInstance(project: Project): UpdateHistoryService = project.service()
  }
}

data class UpdateItem(val versionBefore: String, val versionAfter: String, val updater: UpdaterImplementation) {

  override fun toString(): String {
    return "$versionBefore->$versionAfter" + if (updater != UpdaterImplementation.COLLECT_UPDATE) "(updater $updater)" else ""
  }
}

enum class UpdaterImplementation {
  /**
   * Initial updater implementation
   */
  FIRST,

  /**
   * Implementation of 2025 with coroutines and collect/update logic
   */
  COLLECT_UPDATE
}