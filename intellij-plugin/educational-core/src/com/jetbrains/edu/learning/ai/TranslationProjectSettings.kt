package com.jetbrains.edu.learning.ai

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.ai.TranslationProjectSettings.TranslationProjectState
import com.jetbrains.educational.core.enum.Language
import com.jetbrains.educational.translation.format.domain.TranslationVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

fun Project.translationSettings(): TranslationProjectSettings = service()

@Service(Service.Level.PROJECT)
@State(name = "TranslationProjectSettings", reloadable = true, storages = [Storage("edu_translation.xml")])
class TranslationProjectSettings : PersistentStateComponent<TranslationProjectState> {
  private val _translationLanguage = MutableStateFlow<Language?>(null)
  val translationLanguage: StateFlow<Language?> = _translationLanguage.asStateFlow()
  private val translationLanguageVersions = ConcurrentHashMap<Language, TranslationVersion>()

  fun setTranslation(properties: TranslationProperties?) {
    if (properties == null) {
      _translationLanguage.value = null
      return
    }
    val (language, version) = properties
    _translationLanguage.value = language
    translationLanguageVersions[language] = version
  }

  fun getTranslationVersion(language: Language): TranslationVersion? = translationLanguageVersions[language]

  override fun getState(): TranslationProjectState {
    val state = TranslationProjectState()
    state.currentTranslationLanguage = translationLanguage.value
    state.translationVersions = translationLanguageVersions.mapValuesTo(mutableMapOf()) { (_, value) -> value.value }
    return state
  }

  override fun loadState(state: TranslationProjectState) {
    translationLanguageVersions.clear()
    for ((language, version) in state.translationVersions) {
      translationLanguageVersions[language] = TranslationVersion(version)
    }
    val language = state.currentTranslationLanguage ?: return
    if (getTranslationVersion(language) != null) {
      _translationLanguage.value = language
    }
  }

  class TranslationProjectState : BaseState() {
    var currentTranslationLanguage by enum<Language>()
    var translationVersions by map<Language, Int>()
  }

  companion object {
    fun getCurrentTranslationLanguage(project: Project): Language? = project.translationSettings().state.currentTranslationLanguage

    fun isCourseTranslated(project: Project): Boolean = getCurrentTranslationLanguage(project) != null

    fun resetTranslation(project: Project) {
      project.translationSettings().setTranslation(null)
    }
  }
}