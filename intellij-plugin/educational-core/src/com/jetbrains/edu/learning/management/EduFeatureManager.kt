package com.jetbrains.edu.learning.management

import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import kotlinx.serialization.Serializable

@Service(Service.Level.PROJECT)
@State(name = "CourseFeatures", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class EduFeatureManager : SerializablePersistentStateComponent<EduFeatureManager.CourseFeatureState>(CourseFeatureState()) {

  val featureState: Map<EduManagedFeature, Boolean>
    get() = state.features

  fun updateManagerState(state: CourseFeatureState) {
    updateState { state }
  }

  fun checkEnabled(featureId: EduManagedFeature): Boolean = state.features[featureId] ?: true


  @Serializable
  data class CourseFeatureState(val features: Map<EduManagedFeature, Boolean> = emptyMap())
}