package com.jetbrains.edu.learning.plugins

import com.intellij.openapi.extensions.PluginId

data class PluginInfo(val stringId: String, val displayName: String) {
  val id: PluginId get() = PluginId.getId(stringId)

  companion object {
    val JAVA: PluginInfo = PluginInfo("com.intellij.java", "Java")
    val KOTLIN: PluginInfo = PluginInfo("org.jetbrains.kotlin", "Kotlin")
    val SCALA: PluginInfo = PluginInfo("org.intellij.scala", "Scala")
    // Since 193 is named `Gradle-Java`
    val GRADLE: PluginInfo = PluginInfo("org.jetbrains.plugins.gradle", "Gradle")
    val JUNIT: PluginInfo = PluginInfo("JUnit", "JUnit")

    val ANDROID : PluginInfo = PluginInfo("org.jetbrains.android", "Android")

    val PYTHON_PRO: PluginInfo = PluginInfo("Pythonid", "Python")
    val PYTHON_COMMUNITY: PluginInfo = PluginInfo("PythonCore", "Python")

    val JAVA_SCRIPT: PluginInfo = PluginInfo("JavaScript", "JavaScript")
    val JAVA_SCRIPT_DEBUGGER: PluginInfo = PluginInfo("JavaScriptDebugger", "JavaScript Debugger")
    val NODE_JS: PluginInfo = PluginInfo("NodeJS", "NodeJS")

    val RUST: PluginInfo = PluginInfo("org.rust.lang", "Rust")
    val TOML: PluginInfo = PluginInfo("org.toml.lang", "Toml")

    val CATCH: PluginInfo = PluginInfo("org.jetbrains.plugins.clion.test.catch", "Catch")
    val GOOGLE_TEST: PluginInfo = PluginInfo("org.jetbrains.plugins.clion.test.google", "Google Test")

    val GO: PluginInfo = PluginInfo("org.jetbrains.plugins.go", "Go")
  }
}
