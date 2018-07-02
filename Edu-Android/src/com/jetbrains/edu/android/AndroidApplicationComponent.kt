package com.jetbrains.edu.android

import com.intellij.openapi.components.ApplicationComponent

class AndroidApplicationComponent : ApplicationComponent {

  override fun initComponent() {
    // Need to load Android class and invoke its constructor to register it in IDEA
    Android
  }
}
