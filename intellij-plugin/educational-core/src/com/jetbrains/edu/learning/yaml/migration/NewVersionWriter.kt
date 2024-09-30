package com.jetbrains.edu.learning.yaml.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.YAML_VERSION

class NewVersionWriter(val version: Int) : YamlMigrationStep {
  override fun getConfigName() = COURSE_CONFIG
  override fun transform(configTree: ObjectNode): ObjectNode = configTree.put(YAML_VERSION, version)
}