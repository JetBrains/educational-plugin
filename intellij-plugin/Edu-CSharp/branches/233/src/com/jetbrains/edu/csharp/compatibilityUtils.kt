package com.jetbrains.edu.csharp


const val SDK_VERSION_80 = "SDK 8.0"
const val SDK_VERSION_70 = "SDK 7.0"
const val DOTNET_VERSION_80 = "net8.0"
const val DOTNET_VERSION_70 = "net7.0"
fun getDotNetVersion(version: String?): String = when (version) { // more versions to be added
  SDK_VERSION_70 -> DOTNET_VERSION_70
  else -> DOTNET_VERSION_80
}