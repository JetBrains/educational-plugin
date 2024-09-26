package com.jetbrains.edu.learning

enum class JavaUILibrary {
  SWING {
    override fun toString() = "Swing"
  },
  @Deprecated("JavaFX is no longer supported")
  JAVAFX {
    override fun toString() = "JavaFX"
  },
  JCEF {
    override fun toString() = "JCEF"
  };

  @Suppress("unused", "MemberVisibilityCanBePrivate")
  companion object {
    fun isSwing(): Boolean = EduSettings.getInstance().javaUiLibrary == SWING
    fun isJCEF(): Boolean = EduSettings.getInstance().javaUiLibrary == JCEF
  }
}