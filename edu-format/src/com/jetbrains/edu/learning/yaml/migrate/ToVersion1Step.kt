package com.jetbrains.edu.learning.yaml.migrate

import com.fasterxml.jackson.databind.node.ObjectNode

object ToVersion1Step: YamlMigrationStep {
  override fun migrateCourse(config: ObjectNode) = config
}