package com.jetbrains.edu.learning.ai.terms

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import com.intellij.util.xmlb.annotations.XCollection
import com.jetbrains.edu.learning.EduTestAware
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.ai.terms.TermsProjectSettings.TermsProjectState
import com.jetbrains.educational.terms.format.Term
import com.jetbrains.educational.terms.format.domain.TermsVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.iterator
import kotlin.collections.set

@Service(Service.Level.PROJECT)
@State(name = "TermsProjectSettings", reloadable = true, storages = [Storage("edu_terms.xml")])
class TermsProjectSettings : PersistentStateComponent<TermsProjectState>, EduTestAware {
  private val _termsProperties = MutableStateFlow<TermsProperties?>(null)
  val termsProperties = _termsProperties.asStateFlow()
  private val termsPropertiesByLanguage = ConcurrentHashMap<String, TermsProperties>()

  fun getTaskTerms(task: Task): List<Term>? = termsProperties.value?.terms?.get(task.id)

  fun setTerms(termsProperties: TermsProperties?) {
    if (termsProperties == null) {
      _termsProperties.value = null
      return
    }
    val language = termsProperties.languageCode
    termsPropertiesByLanguage[language] = termsProperties
    _termsProperties.value = termsProperties
  }

  fun getTermsByLanguage(languageCode: String): TermsProperties? = termsPropertiesByLanguage[languageCode]

  override fun getState(): TermsProjectState {
    val state = TermsProjectState()
    state.terms = termsPropertiesByLanguage.mapValuesTo(mutableMapOf()) { (_, properties) ->
      properties.terms.mapValues { (_, taskTerms) ->
        taskTerms.map { it.asStoredTerm() }
      }
    }
    state.termsVersions = termsPropertiesByLanguage.mapValuesTo(mutableMapOf()) { (_, properties) -> properties.version.value }
    state.currentTermsLanguage = termsProperties.value?.languageCode
    return state
  }

  override fun loadState(state: TermsProjectState) {
    termsPropertiesByLanguage.clear()

    for ((language, version) in state.termsVersions) {
      val terms = state.terms[language] ?: emptyMap()
      val taskTerms = mutableMapOf<Int, List<Term>>()
      for ((taskId, storedTaskTerms) in terms) {
        taskTerms[taskId] = storedTaskTerms.map { it.asTerm() }
      }

      termsPropertiesByLanguage[language] = TermsProperties(language, taskTerms, TermsVersion(version))
    }

    val language = state.currentTermsLanguage ?: return
    _termsProperties.value = getTermsByLanguage(language)
  }

  fun resetTerms() {
    _termsProperties.value = null
    termsPropertiesByLanguage.clear()
  }

  @TestOnly
  override fun cleanUpState() {
    resetTerms()
  }

  @Tag("Term")
  data class StoredTerm(
    @Attribute var value: String,
    @Attribute var definition: String
  ) {
    fun asTerm(): Term = Term(value, definition)

    @Suppress("unused") // used for serialization
    constructor(): this("", "")
  }

  private fun Term.asStoredTerm(): StoredTerm = StoredTerm(value, definition)

  class TermsProjectState : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    var currentTermsLanguage by string()

    @get:XCollection(style = XCollection.Style.v2)
    var terms by map<String, Map<Int, List<StoredTerm>>>()

    @get:XCollection(style = XCollection.Style.v2)
    var termsVersions by map<String, Int>()
  }

  companion object {
    fun getInstance(project: Project): TermsProjectSettings = project.service()

    fun areCourseTermsLoaded(project: Project): Boolean = getInstance(project).termsProperties.value != null

    fun areCourseTermsLoaded(project: Project, languageCode: String): Boolean {
      return getInstance(project).termsProperties.value?.languageCode == languageCode
    }
  }
}