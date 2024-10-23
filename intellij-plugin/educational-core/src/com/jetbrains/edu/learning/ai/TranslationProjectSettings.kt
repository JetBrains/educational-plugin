package com.jetbrains.edu.learning.ai

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.ai.TranslationProjectSettings.TranslationProjectState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import org.jetbrains.annotations.VisibleForTesting

@Service(Service.Level.PROJECT)
@State(name="TranslationProjectSettings", reloadable = true, storages = [Storage("edu_translation.xml")])
class TranslationProjectSettings : SimplePersistentStateComponent<TranslationProjectState>(TranslationProjectState()) {
  private val translationLanguageCodeChangeFlow = MutableStateFlow(currentTranslationLanguageCode)

  @get:VisibleForTesting
  var currentTranslationLanguageCode: String?
    get() = state.currentTranslationLanguageCode
    set(value) {
      if (value == currentTranslationLanguageCode) return
      state.currentTranslationLanguageCode = value
      translationLanguageCodeChangeFlow.value = value
    }

  class TranslationProjectState : BaseState() {
    var currentTranslationLanguageCode by string()
  }

  companion object {
    fun getInstance(project: Project) = project.service<TranslationProjectSettings>()

    fun getCurrentTranslationLanguageCode(project: Project): String? = getInstance(project).currentTranslationLanguageCode

    fun setCurrentTranslationLanguageCode(project: Project, languageCode: String?) {
      getInstance(project).currentTranslationLanguageCode = languageCode
    }

    fun isCourseTranslated(project: Project): Boolean = getCurrentTranslationLanguageCode(project) != null

    fun resetTranslation(project: Project) {
      setCurrentTranslationLanguageCode(project, null)
    }

    fun getTranslationLanguageCodeChangeFlow(project: Project): Flow<String?> =
      // drop the very first emission due to the loading of TranslationProjectState object
      getInstance(project).translationLanguageCodeChangeFlow.drop(1)
  }
}