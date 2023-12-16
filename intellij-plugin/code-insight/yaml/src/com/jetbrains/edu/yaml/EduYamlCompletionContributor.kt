package com.jetbrains.edu.yaml

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.Language
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.PsiElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.edu.codeInsight.inFileWithName
import com.jetbrains.edu.codeInsight.psiElement
import com.jetbrains.edu.learning.configuration.EduConfiguratorManager
import com.jetbrains.edu.learning.courseFormat.EduFormatNames
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.yaml.YamlConfigSettings
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl
import java.util.*

class EduYamlCompletionContributor : CompletionContributor() {
  init {
    extendCompletionForProgrammingLanguage()
    extendCompletionForProgrammingLanguageVersion()
    extendCompletionForHumanLanguage()
    extendCompletionForEnvironment()
  }

  private fun extendCompletionForEnvironment() {
    extendCompletionForKey(ENVIRONMENT) { yamlFile ->
      val language = yamlFile.programmingLanguage ?: return@extendCompletionForKey emptyList()
      // "null" for default environment will be added by schema completion
      EduConfiguratorManager.supportedEnvironments(language).filter { it != DEFAULT_ENVIRONMENT }
    }
  }

  private fun extendCompletionForHumanLanguage() {
    extendCompletionForKey(LANGUAGE) {
      Locale.getISOLanguages().map { Locale(it).displayName }
    }
  }

  private fun extendCompletionForProgrammingLanguageVersion() {
    extendCompletionForKey(PROGRAMMING_LANGUAGE_VERSION) { yamlFile ->
      val language = yamlFile.programmingLanguage ?: return@extendCompletionForKey emptyList()
      val supportedLanguageVersions = EduConfiguratorManager.findConfigurator(EduFormatNames.PYCHARM, DEFAULT_ENVIRONMENT,
                                                                              language)?.courseBuilder?.getSupportedLanguageVersions()
      return@extendCompletionForKey supportedLanguageVersions.orEmpty()
    }
  }

  private val YAMLFile.programmingLanguage: Language?
    get() {
      val languageDisplayName = YAMLUtil.getValue(this, PROGRAMMING_LANGUAGE)?.second
      return Language.getRegisteredLanguages().find { it?.displayName == languageDisplayName }
    }

  private fun extendCompletionForProgrammingLanguage() {
    val supportedLanguages = EduConfiguratorManager.supportedEduLanguages.toSet().map {
      Language.findLanguageByID(it)!!.displayName
    }
    extendCompletionForKey(PROGRAMMING_LANGUAGE) { supportedLanguages }
  }

  private fun createPatternForKey(keyText: String): PsiElementPattern.Capture<PsiElement> =
    psiElement()
      .inFileWithName(YamlConfigSettings.COURSE_CONFIG)
      .withParent(
        psiElement<YAMLPlainTextImpl>().withParent(keyValueWithName(keyText))
      )

  private fun extendCompletionForKey(keyText: String, lookupStrings: (YAMLFile) -> List<String>) {
    extend(CompletionType.BASIC, createPatternForKey(keyText), object : CompletionProvider<CompletionParameters>() {
      override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
        val yamlFile = parameters.originalFile as? YAMLFile ?: return
        result.addAllElements(lookupStrings(yamlFile).map { LookupElementBuilder.create(it) })
      }
    })
  }
}