package com.jetbrains.edu.csharp

import com.jetbrains.rd.ide.model.RdPostProcessParameters
import com.jetbrains.rider.model.AddProjectCommand

fun createAddProjectCommand(parentId: Int, taskPaths: List<String>, params: RdPostProcessParameters): AddProjectCommand {
  return AddProjectCommand(parentId, taskPaths, listOf(), true, params)
}
