package com.jetbrains.edu.learning.featureManagement

import com.intellij.openapi.components.RoamingType
import com.intellij.openapi.components.SerializablePersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.jetbrains.edu.learning.EduTestAware
import kotlinx.serialization.Serializable

@Service(Service.Level.PROJECT)
@State(name = "CourseFeatures", storages = [Storage(StoragePathMacros.WORKSPACE_FILE, roamingType = RoamingType.DISABLED)])
class EduFeatureManager : SerializablePersistentStateComponent<EduFeatureManager.CourseFeatureState>(CourseFeatureState()), EduTestAware {

  fun updateManagerState(state: Set<EduManagedFeature>) {
    updateState { CourseFeatureState(state) }
  }

  fun checkDisabled(featureId: EduManagedFeature): Boolean = featureId in state.disabledFeatures

  override fun cleanUpState() {
    updateManagerState(emptySet())
  }

  @Serializable
  data class CourseFeatureState(val disabledFeatures: Set<EduManagedFeature> = emptySet())
}