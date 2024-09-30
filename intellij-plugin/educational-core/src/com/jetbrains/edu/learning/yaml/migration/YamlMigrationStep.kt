package com.jetbrains.edu.learning.yaml.migration

import com.fasterxml.jackson.databind.node.ObjectNode

interface YamlMigrationStep {
  fun getConfigName(): String
  fun transform(config: ObjectNode): ObjectNode
}