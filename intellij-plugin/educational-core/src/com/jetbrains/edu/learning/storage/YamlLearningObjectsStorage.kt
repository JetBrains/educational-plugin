package com.jetbrains.edu.learning.storage

class YamlLearningObjectsStorage : InMemoryLearningObjectsStorage() {
  override val writeTextInYaml = true
}