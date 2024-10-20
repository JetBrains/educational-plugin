package com.jetbrains.edu.ai.translation.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.ai.translation.settings.TranslationProjectSettings.TranslationProjectState
import com.jetbrains.educational.translation.enum.Language

@Service(Service.Level.PROJECT)
@State(name="TranslationProjectSettings", reloadable = true, storages = [Storage("edu_translation.xml")])
class TranslationProjectSettings : SimplePersistentStateComponent<TranslationProjectState>(TranslationProjectState()) {
  var translatedToLanguage: Language?
    get() = state.translatedToLanguage
    set(value) {
      state.translatedToLanguage = value
    }

  class TranslationProjectState : BaseState() {
    var translatedToLanguage by enum<Language>()
  }

  companion object {
    fun getInstance(project: Project) = project.service<TranslationProjectSettings>()
  }
}