package com.jetbrains.edu.learning.statistics

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.statistics.CoursePageExperimentManager.ExperimentState
import kotlinx.serialization.Serializable


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
