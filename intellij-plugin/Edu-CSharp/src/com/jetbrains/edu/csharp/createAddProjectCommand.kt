package com.jetbrains.edu.csharp

import com.jetbrains.rd.ide.model.RdPostProcessParameters
import com.jetbrains.rider.ijent.extensions.toRdPath
import com.jetbrains.rider.model.AddProjectCommand

// BACKCOMPAT: 2025.3. Inline it
fun createAddProjectCommand(parentId: Int, taskPaths: List<String>, params: RdPostProcessParameters): AddProjectCommand {
  return AddProjectCommand(parentId, taskPaths.map { it.toRdPath() }, listOf(), true, params)
}
