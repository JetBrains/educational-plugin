package com.jetbrains.edu.learning.statistics.metadata

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.statistics.metadata.CourseSubmissionMetadataManager.MetadataState
import kotlinx.serialization.Serializable

/**
 * Storage for course related metadata collected during course creation/opening which supposed to be attached to submissions
 */
@Service(Service.Level.PROJECT)
@State(name = "CourseSubmissionMetadataManager", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class CourseSubmissionMetadataManager : SerializablePersistentStateComponent<MetadataState>(MetadataState()), EduTestAware {

  val metadata: Map<String, String>
    get() = state.metadata

  fun addMetadata(metadata: Map<String, String>) {
    updateState { current -> MetadataState(current.metadata + metadata) }
  }

  fun addMetadata(vararg metadata: Pair<String, String>) {
    updateState { current -> MetadataState(current.metadata + metadata) }
  }

  override fun cleanUpState() {
    updateState { MetadataState() }
  }

  companion object {
    const val EXPERIMENT_ID = "experiment_id"
    const val EXPERIMENT_VARIANT = "experiment_variant"

    const val ENTRY_POINT = "entry_point"

    const val MAX_VALUE_LENGTH = 16

    fun getInstance(project: Project): CourseSubmissionMetadataManager = project.service()
  }

  @Serializable
  data class MetadataState(val metadata: Map<String, String> = emptyMap())
}
