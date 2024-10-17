package com.jetbrains.edu.learning.yaml.migrate

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.yaml.YamlMapper.CURRENT_YAML_VERSION
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.YAML_VERSION

class YamlMigrator(mapper: ObjectMapper) {

  val migrationSteps = mapOf(
    1 to ToVersion1Step,
    2 to ToVersion2AddAdditionalFiles(mapper)
  )

  fun migrateCourse(configTree: ObjectNode): ObjectNode {
    val yamlVersion = configTree.get(YAML_VERSION).asInt(0)

    var migratedConfig = configTree
    for (version in yamlVersion..CURRENT_YAML_VERSION) {
      val nextStep = migrationSteps[version]
      migratedConfig = nextStep?.migrateCourse(migratedConfig) ?: migratedConfig
    }

    return migratedConfig
  }
}