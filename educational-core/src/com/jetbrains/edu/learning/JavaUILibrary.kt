package com.jetbrains.edu.learning

enum class JavaUILibrary {
  SWING {
    override fun toString() = "Swing"
  },
  JAVAFX {
    override fun toString() = "JavaFX"
  },
  JCEF {
    override fun toString() = "JCEF"
  };

  @Suppress("unused", "MemberVisibilityCanBePrivate")
  companion object {
    fun isSwing(): Boolean = EduSettings.getInstance().javaUiLibraryWithCheck == SWING
    fun isJavaFx(): Boolean = EduSettings.getInstance().javaUiLibraryWithCheck == JAVAFX
    fun isJCEF(): Boolean = EduSettings.getInstance().javaUiLibraryWithCheck == JCEF
    fun isJavaFxOrJCEF(): Boolean = isJavaFx() || isJCEF()
  }
}