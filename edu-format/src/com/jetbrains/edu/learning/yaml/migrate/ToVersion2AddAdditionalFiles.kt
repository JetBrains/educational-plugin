package com.jetbrains.edu.learning.yaml.migrate

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import com.jetbrains.edu.learning.courseFormat.EduFile
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.ADDITIONAL_FILES
import com.jetbrains.edu.learning.yaml.AdditionalFilesProvider
import com.jetbrains.edu.learning.yaml.InjectableValueKey
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.NAME
import com.jetbrains.edu.learning.yaml.getEduValue
import kotlin.collections.forEach

object AdditionalFilesProviderKey: InjectableValueKey<AdditionalFilesProvider>("additional_files_provider")

class ToVersion2AddAdditionalFiles(val mapper: ObjectMapper): YamlMigrationStep {

  private val additionalFilesProvider = mapper.getEduValue(AdditionalFilesProviderKey)

  override fun migrateCourse(config: ObjectNode): ObjectNode {
    val additionalFiles = getAdditionalFiles(config)

    val additionalFilesNode = mapper.createArrayNode()
    additionalFiles.forEach { additionalFile ->
      val additionalFileNode = mapper.createObjectNode()
      additionalFileNode.replace(NAME, TextNode(additionalFile.name))
      additionalFilesNode.add(additionalFileNode)
    }

    config.replace(ADDITIONAL_FILES, additionalFilesNode)
    return config
  }

  private fun getAdditionalFiles(config: ObjectNode): List<EduFile> {
    val courseType = config.
    val environment = "asdf"
    val languageId = "asdf"

    return additionalFilesProvider?.let {
      it(courseType, environment, languageId)
    } ?: emptyList()
  }
}
