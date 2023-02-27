package com.jetbrains.edu.learning.newproject.ui

import com.intellij.openapi.extensions.ExtensionPointName

/**
 * @property getProviders specifies the list of [CoursesPlatformProvider]. Each provider gives information for the tab on
 * [CoursesPanelWithTabs], e.g icon, list of courses.
 */
interface CoursesPlatformProviderFactory {

  fun getProviders(): List<CoursesPlatformProvider>

  companion object {
    private val EP_NAME = ExtensionPointName.create<CoursesPlatformProviderFactory>("Educational.coursesPlatformProviderFactory")

    val allProviders: List<CoursesPlatformProvider> get() = EP_NAME.extensionList.flatMap { it.getProviders() }
  }

}
