package com.jetbrains.edu.csharp.checker

import com.jetbrains.rider.build.BuildParameters
import com.jetbrains.rider.model.BuildTarget

// BACKCOMPAT: 2026.1. Inline it
fun getBuildParameters(projectFilePaths: List<String>): BuildParameters =
  BuildParameters(
    operation = BuildTarget(),
    selectedProjectsPaths = projectFilePaths,
    silentMode = true,
    diagnosticsMode = false,
    withoutDependencies = false,
    noRestore = false
  )