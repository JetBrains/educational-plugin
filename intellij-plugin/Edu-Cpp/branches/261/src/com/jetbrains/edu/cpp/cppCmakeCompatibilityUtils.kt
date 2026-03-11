package com.jetbrains.edu.cpp

import com.jetbrains.cidr.cpp.toolchains.CMakeExecutableTool
import java.io.File

fun getBundledCMakeToolBinary(): File {
  return CMakeExecutableTool.getBundledCMakeToolBinary(CMakeExecutableTool.ToolKind.CMAKE)
}