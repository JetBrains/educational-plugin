package com.jetbrains.edu.csharp

import com.jetbrains.rider.projectView.projectTemplates.components.ProjectTemplateSdk
import com.jetbrains.rider.projectView.projectTemplates.components.ProjectTemplateTargetFramework

val SDK_VERSION_80 = ProjectTemplateSdk.net8.presentation
val SDK_VERSION_70 = ProjectTemplateSdk.net7.presentation

fun getDotNetVersion(version: String?): String = when (version) { // more versions to be added
  ProjectTemplateSdk.net7.presentation -> ProjectTemplateTargetFramework.net70.presentation
  else -> ProjectTemplateTargetFramework.net80.presentation
}