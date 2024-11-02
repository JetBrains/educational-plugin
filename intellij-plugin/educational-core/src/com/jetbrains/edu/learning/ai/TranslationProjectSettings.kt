package com.jetbrains.edu.learning.ai

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.ai.TranslationProjectSettings.TranslationProjectState
import com.jetbrains.educational.core.enum.TranslationLanguage
import com.jetbrains.educational.translation.format.domain.TranslationVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

fun Project.translationSettings(): TranslationProjectSettings = service()

@Service(Service.Level.PROJECT)
@State(name = "TranslationProjectSettings", reloadable = true, storages = [Storage("edu_translation.xml")])
class TranslationProjectSettings : PersistentStateComponent<TranslationProjectState> {
  private val _translationProperties = MutableStateFlow<TranslationProperties?>(null)
  val translationProperties = _translationProperties.asStateFlow()
  private val translationLanguageVersions = ConcurrentHashMap<TranslationLanguage, TranslationVersion>()

  fun setTranslation(properties: TranslationProperties?) {
    if (properties == null) {
      _translationProperties.value = null
      return
    }
    val (language, version) = properties
    translationLanguageVersions[language] = version
    _translationProperties.value = properties
  }

  fun getTranslationProperties(): TranslationProperties? = translationProperties.value

  fun getTranslationVersion(): TranslationVersion? = translationProperties.value?.version

  override fun getState(): TranslationProjectState {
    val state = TranslationProjectState()
    state.currentTranslationLanguage = translationProperties.value?.language
    state.translationVersions = translationLanguageVersions.mapValuesTo(mutableMapOf()) { (_, value) -> value.value }
    return state
  }

  override fun loadState(state: TranslationProjectState) {
    translationLanguageVersions.clear()
    for ((language, version) in state.translationVersions) {
      translationLanguageVersions[language] = TranslationVersion(version)
    }
    val language = state.currentTranslationLanguage ?: return
    val version = translationLanguageVersions[language] ?: return
    _translationProperties.value = TranslationProperties(language, version)
  }

  class TranslationProjectState : BaseState() {
    var currentTranslationLanguage by enum<TranslationLanguage>()
    var translationVersions by map<TranslationLanguage, Int>()
  }

  companion object {
    fun getCurrentTranslationLanguage(project: Project): TranslationLanguage? =
      project.translationSettings().state.currentTranslationLanguage

    fun isCourseTranslated(project: Project): Boolean = getCurrentTranslationLanguage(project) != null

    fun resetTranslation(project: Project) {
      project.translationSettings().setTranslation(null)
    }
  }
}