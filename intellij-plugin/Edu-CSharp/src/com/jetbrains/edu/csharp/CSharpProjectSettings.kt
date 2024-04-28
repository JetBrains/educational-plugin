package com.jetbrains.edu.csharp

import com.jetbrains.edu.learning.newproject.EduProjectSettings
import com.jetbrains.rider.projectView.projectTemplates.components.ProjectTemplateSdk
data class CSharpProjectSettings(val version: String = ProjectTemplateSdk.net8.presentation) : EduProjectSettings
