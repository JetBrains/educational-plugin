package com.jetbrains.edu.rust.actions

import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.project.workspace.CargoWorkspaceData

// BACKCOMPAT: 2026.1. Inline it
fun createTargetForMain(contentRoot: String): CargoWorkspaceData.Target {
  return CargoWorkspaceData.Target("$contentRoot/lesson1/task1/main.rs", "task1", CargoWorkspace.TargetKind.Bin,
    edition = CargoWorkspace.Edition.EDITION_2015, doctest = false, requiredFeatures = emptyList(), harness = true)
}