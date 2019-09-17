package com.jetbrains.edu.learning

import com.intellij.openapi.application.Experiments

fun isFeatureEnabled(featureId: String): Boolean = Experiments.getInstance().isFeatureEnabled(featureId)
