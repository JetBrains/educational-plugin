package com.jetbrains.edu.learning.ai

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.ai.TranslationProjectSettings.TranslationProjectState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

fun Project.translationSettings(): TranslationProjectSettings = service()

@Service(Service.Level.PROJECT)
@State(name="TranslationProjectSettings", reloadable = true, storages = [Storage("edu_translation.xml")])
class TranslationProjectSettings : PersistentStateComponent<TranslationProjectState> {
  private val _translationLanguageCodeChange = MutableStateFlow<String?>(null)
  val translationLanguageCodeChange: StateFlow<String?> = _translationLanguageCodeChange.asStateFlow()

  fun setCurrentTranslationLanguageCode(languageCode: String?) {
    _translationLanguageCodeChange.value = languageCode
  }

  override fun getState(): TranslationProjectState {
    val state = TranslationProjectState()
    state.currentTranslationLanguageCode = translationLanguageCodeChange.value
    return state
  }

  override fun loadState(state: TranslationProjectState) {
    _translationLanguageCodeChange.value = state.currentTranslationLanguageCode
  }

  class TranslationProjectState : BaseState() {
    var currentTranslationLanguageCode by string()
  }

  companion object {
    fun getCurrentTranslationLanguageCode(project: Project): String? = project.translationSettings().state.currentTranslationLanguageCode

    fun isCourseTranslated(project: Project): Boolean = getCurrentTranslationLanguageCode(project) != null

    fun resetTranslation(project: Project) {
      project.translationSettings().setCurrentTranslationLanguageCode(null)
    }
  }
}