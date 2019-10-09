package com.jetbrains.edu.learning

import com.intellij.openapi.application.Experiments

fun isFeatureEnabled(featureId: String): Boolean = Experiments.isFeatureEnabled(featureId)
fun setFeatureEnabled(featureId: String, enabled: Boolean) = Experiments.setFeatureEnabled(featureId, enabled)
