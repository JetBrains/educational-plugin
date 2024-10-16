package com.jetbrains.edu.learning.yaml.migrate

import com.fasterxml.jackson.databind.node.ObjectNode

interface YamlMigrationStep {
  fun migrateCourse(config: ObjectNode): ObjectNode
}