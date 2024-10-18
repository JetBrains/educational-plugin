package com.jetbrains.edu.learning.yaml.migrate

/**
 * A first migration step does no real migration but is needed to include "version=1" inside course_info.yaml
 */
object ToVersion1Step: YamlMigrationStep
