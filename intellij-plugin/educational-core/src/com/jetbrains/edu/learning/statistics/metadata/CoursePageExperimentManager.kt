@file:Suppress("DEPRECATION")

package com.jetbrains.edu.learning.statistics.metadata

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.statistics.metadata.CoursePageExperimentManager.ExperimentState
import kotlinx.serialization.Serializable


// BACKCOMPAT: 2025.1. Drop it
@Deprecated("Use `CourseMetadataManager` instead")
@Service(Service.Level.PROJECT)
@State(name = "CoursePageExperimentManager", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class CoursePageExperimentManager : SerializablePersistentStateComponent<ExperimentState>(ExperimentState(null)), EduTestAware {

  var experiment: CoursePageExperiment?
    get() = state.experiment
    set(value) {
      updateState { ExperimentState(value) }
    }

  override fun cleanUpState() {
    experiment = null
  }

  companion object {
    fun getInstance(project: Project): CoursePageExperimentManager = project.service()
  }

  @Serializable
  data class ExperimentState(val experiment: CoursePageExperiment?)
}
