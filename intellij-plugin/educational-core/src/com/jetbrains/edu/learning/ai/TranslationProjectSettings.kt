package com.jetbrains.edu.learning.ai

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.ai.TranslationProjectSettings.TranslationProjectState
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.educational.core.enum.TranslationLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

fun Project.translationSettings(): TranslationProjectSettings = service()

@Service(Service.Level.PROJECT)
@State(name = "TranslationProjectSettings", reloadable = true, storages = [Storage("edu_translation.xml")])
class TranslationProjectSettings : PersistentStateComponent<TranslationProjectState> {
  private val _translationProperties = MutableStateFlow<TranslationProperties?>(null)
  val translationProperties = _translationProperties.asStateFlow()
  private val structureTranslations = ConcurrentHashMap<TranslationLanguage, Map<String, String>>()
  private val translationLanguageVersions = ConcurrentHashMap<TranslationLanguage, Int>()

  fun setTranslation(properties: TranslationProperties?) {
    if (properties == null) {
      _translationProperties.value = null
      return
    }
    val language = properties.language
    structureTranslations[language] = properties.structureTranslation
    translationLanguageVersions[language] = properties.version
    _translationProperties.value = properties
  }

  val translationLanguage: TranslationLanguage?
    get() = translationProperties.value?.language

  fun getTranslationPropertiesByLanguage(language: TranslationLanguage): TranslationProperties? {
    val structure = structureTranslations[language] ?: return null
    val version = translationLanguageVersions[language] ?: return null
    return TranslationProperties(language, structure, version)
  }

  fun getStudyItemTranslatedName(item: StudyItem): String? {
    val language = translationProperties.value?.language ?: return null
    return structureTranslations[language]?.get(item.id.toString())
  }

  override fun getState(): TranslationProjectState {
    val state = TranslationProjectState()
    state.currentTranslationLanguage = translationProperties.value?.language
    state.structureTranslation = structureTranslations
    state.translationVersions = translationLanguageVersions
    return state
  }

  override fun loadState(state: TranslationProjectState) {
    structureTranslations.clear()
    for ((language, structure) in state.structureTranslation) {
      structureTranslations[language] = structure
    }
    translationLanguageVersions.clear()
    for ((language, version) in state.translationVersions) {
      translationLanguageVersions[language] = version
    }

    val language = state.currentTranslationLanguage ?: return
    _translationProperties.value = getTranslationPropertiesByLanguage(language)
  }

  class TranslationProjectState : BaseState() {
    var currentTranslationLanguage by enum<TranslationLanguage>()
    var structureTranslation by map<TranslationLanguage, Map<String, String>>()
    var translationVersions by map<TranslationLanguage, Int>()
  }

  companion object {
    fun isCourseTranslated(project: Project): Boolean = project.translationSettings().translationProperties.value != null

    fun resetTranslation(project: Project) {
      val settings = project.translationSettings()
      settings._translationProperties.value = null
      settings.structureTranslations.clear()
      settings.translationLanguageVersions.clear()
    }
  }
}