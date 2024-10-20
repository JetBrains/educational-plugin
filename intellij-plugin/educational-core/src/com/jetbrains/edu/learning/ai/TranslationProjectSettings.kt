package com.jetbrains.edu.learning.ai

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.ai.TranslationProjectSettings.TranslationProjectState

@Service(Service.Level.PROJECT)
@State(name="TranslationProjectSettings", reloadable = true, storages = [Storage("edu_translation.xml")])
class TranslationProjectSettings : SimplePersistentStateComponent<TranslationProjectState>(TranslationProjectState()) {
  var currentTranslationLanguageCode: String?
    get() = state.currentTranslationLanguageCode
    set(value) {
      state.currentTranslationLanguageCode = value
    }

  class TranslationProjectState : BaseState() {
    var currentTranslationLanguageCode by string()
  }

  companion object {
    fun getInstance(project: Project) = project.service<TranslationProjectSettings>()

    fun getCurrentTranslationLanguageCode(project: Project): String? = getInstance(project).currentTranslationLanguageCode

    fun resetTranslation(project: Project) {
      getInstance(project).currentTranslationLanguageCode = null
    }
  }
}