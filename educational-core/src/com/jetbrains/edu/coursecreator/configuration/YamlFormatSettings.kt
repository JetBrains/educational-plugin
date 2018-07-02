package com.jetbrains.edu.coursecreator.configuration

import com.intellij.openapi.application.Experiments


object YamlFormatSettings {
  private const val FEATURE_ID = "edu.course.creator.yaml"

  fun isEnabled() = Experiments.isFeatureEnabled(FEATURE_ID);
}