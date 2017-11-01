package com.jetbrains.edu.learning.intellij

import com.intellij.openapi.roots.ui.configuration.JdkComboBox
import com.intellij.openapi.roots.ui.configuration.projectRoot.ProjectSdksModel

data class JdkProjectSettings(val model: ProjectSdksModel, val jdkItem: JdkComboBox.JdkComboBoxItem?)
