package com.jetbrains.edu.learning.ai

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.ai.TranslationProjectSettings.TranslationProjectState
import com.jetbrains.educational.core.enum.Language
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun Project.translationSettings(): TranslationProjectSettings = service()

@Service(Service.Level.PROJECT)
@State(name="TranslationProjectSettings", reloadable = true, storages = [Storage("edu_translation.xml")])
class TranslationProjectSettings : PersistentStateComponent<TranslationProjectState> {
  private val _translationLanguageChange = MutableStateFlow<Language?>(null)
  val translationLanguageChange: StateFlow<Language?> = _translationLanguageChange.asStateFlow()

  fun setCurrentTranslationLanguage(language: Language?) {
    _translationLanguageChange.value = language
  }

  override fun getState(): TranslationProjectState {
    val state = TranslationProjectState()
    state.currentTranslationLanguage = translationLanguageChange.value
    return state
  }

  override fun loadState(state: TranslationProjectState) {
    _translationLanguageChange.value = state.currentTranslationLanguage
  }

  class TranslationProjectState : BaseState() {
    var currentTranslationLanguage by enum<Language>()
  }

  companion object {
    fun getCurrentTranslationLanguage(project: Project): Language? = project.translationSettings().state.currentTranslationLanguage

    fun isCourseTranslated(project: Project): Boolean = getCurrentTranslationLanguage(project) != null

    fun resetTranslation(project: Project) {
      project.translationSettings().setCurrentTranslationLanguage(null)
    }
  }
}