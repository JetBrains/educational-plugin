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
class CourseSubmissionMetadataManager(
  private val project: Project
) : SerializablePersistentStateComponent<MetadataState>(MetadataState()), EduTestAware {

  val metadata: Map<String, String>
    get() = state.metadata

  // BACKCOMPAT: 2025.1. Drop this method together with deprecated services
  @Suppress("DEPRECATION")
  override fun noStateLoaded() {
    super.noStateLoaded()

    val experiment = CoursePageExperimentManager.getInstance(project).experiment
    CoursePageExperimentManager.getInstance(project).experiment = null

    val entryPoint = EntryPointManager.getInstance(project).entryPoint
    EntryPointManager.getInstance(project).entryPoint = null

    val migratedData = mutableMapOf<String, String>()
    migratedData += experiment?.toMetadataMap().orEmpty()
    if (entryPoint != null) {
      migratedData += ENTRY_POINT to entryPoint
    }

    updateState { current -> MetadataState(current.metadata + migratedData) }
  }

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
