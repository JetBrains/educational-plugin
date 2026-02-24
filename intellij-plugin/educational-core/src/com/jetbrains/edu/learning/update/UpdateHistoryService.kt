package com.jetbrains.edu.learning.update

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware

@Service(Service.Level.PROJECT)
@State(name = "EduUpdateHistory", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class UpdateHistoryService : SerializablePersistentStateComponent<UpdateHistoryService.State>(State()), EduTestAware {
  private var updates: List<UpdateItem>
    get() = state.updates
    set(value) {
      updateState {
        it.copy(updates = value)
      }
    }

  fun updateHappened(updateItem: UpdateItem) {
    updates += updateItem
  }

  fun updatesString(): String = state.updates.joinToString(separator = ",")

  fun isEmpty(): Boolean = updates.isEmpty()

  override fun cleanUpState() {
    updates = emptyList()
  }

  data class State(var updates: List<UpdateItem> = mutableListOf())

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