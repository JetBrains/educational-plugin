package com.jetbrains.edu.learning.yaml.migrate

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.PYCHARM
import com.jetbrains.edu.learning.courseFormat.Language
import com.jetbrains.edu.learning.courseFormat.logger
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.yaml.AdditionalFilesProvider
import com.jetbrains.edu.learning.yaml.InjectableValueKey
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.LANGUAGE
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.getEduValue
import kotlin.collections.forEach

object AdditionalFilesProviderKey: InjectableValueKey<AdditionalFilesProvider>("additional_files_provider")

class ToVersion2AddAdditionalFiles(val mapper: ObjectMapper): YamlMigrationStep {

  private val additionalFilesProvider = mapper.getEduValue(AdditionalFilesProviderKey)

  override fun migrateCourse(config: ObjectNode): ObjectNode {
    val additionalFiles = getAdditionalFiles(config) ?: emptyList()

    val additionalFilesNode = mapper.createArrayNode()
    additionalFiles.forEach { additionalFile ->
      val additionalFileNode = mapper.createObjectNode()
      additionalFileNode.replace(NAME, TextNode(additionalFile.name))
      additionalFilesNode.add(additionalFileNode)
    }

    config.replace(ADDITIONAL_FILES, additionalFilesNode)
    return config
  }

  private fun getAdditionalFiles(config: ObjectNode): List<EduFile>? {
    val environment = config.get(ENVIRONMENT)?.asText() ?: DEFAULT_ENVIRONMENT
    val languageId = config.get(LANGUAGE)?.asText()?.let { Language.findLanguageByName(it) }
    if (languageId == null) {
      LOG.severe("Failed to migrate additional files: language id is unknown")
      return emptyList()
    }

    val provider = additionalFilesProvider
    if (provider == null) {
      LOG.severe("Failed to migrate additional files: no provider for additional files is specified")
      return emptyList()
    }

    return provider(PYCHARM, environment, languageId)
  }

  companion object {
    private val LOG = logger<ToVersion2AddAdditionalFiles>()
  }
}
