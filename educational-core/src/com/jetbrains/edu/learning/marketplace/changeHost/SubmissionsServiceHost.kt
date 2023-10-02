package com.jetbrains.edu.learning.marketplace.changeHost

import com.intellij.ide.util.PropertiesComponent
import com.jetbrains.edu.learning.marketplace.SUBMISSIONS_SERVICE_HOST_PROPERTY
import com.jetbrains.edu.learning.marketplace.SUBMISSIONS_SERVICE_PRODUCTION_URL
import com.jetbrains.edu.learning.marketplace.SUBMISSIONS_SERVICE_STAGING_URL

enum class SubmissionsServiceHost(private val visibleName: String, val url: String) {
  PRODUCTION("Production server", SUBMISSIONS_SERVICE_PRODUCTION_URL),
  STAGING("Staging server", SUBMISSIONS_SERVICE_STAGING_URL),
  OTHER("Other", "http://localhost:8080");

  override fun toString(): String = visibleName

  companion object {
    @JvmStatic
    fun getSelectedHost(): SubmissionsServiceHost = values().firstOrNull { it.url == getSelectedUrl() } ?: OTHER

    @JvmStatic
    fun getSelectedUrl(): String = PropertiesComponent.getInstance().getValue(SUBMISSIONS_SERVICE_HOST_PROPERTY, PRODUCTION.url)
  }
}