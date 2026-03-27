package com.jetbrains.edu.learning.featureManagement

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduTestAware
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable

@Service(Service.Level.PROJECT)
@State(name = "EduFeatureManager", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class EduFeatureManager : SerializablePersistentStateComponent<EduFeatureManager.CourseFeatureState>(CourseFeatureState()), EduTestAware {
  private val _disabledFeatures = MutableStateFlow(emptySet<EduManagedFeature>())
  val disabledFeatures: StateFlow<Set<EduManagedFeature>> = _disabledFeatures.asStateFlow()

  fun updateManagerState(state: Set<EduManagedFeature>) {
    updateState { CourseFeatureState(state) }
    _disabledFeatures.value = state
  }

  override fun loadState(state: CourseFeatureState) {
    super.loadState(state)
    _disabledFeatures.value = state.disabledFeatures
  }

  fun checkDisabled(featureId: EduManagedFeature): Boolean = featureId in state.disabledFeatures

  override fun cleanUpState() {
    updateManagerState(emptySet())
  }

  @Serializable
  data class CourseFeatureState(val disabledFeatures: Set<EduManagedFeature> = emptySet())
  companion object {
    fun getInstance(project: Project): EduFeatureManager = project.service()
  }
}