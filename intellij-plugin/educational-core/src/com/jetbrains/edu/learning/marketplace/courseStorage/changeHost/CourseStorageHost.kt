package com.jetbrains.edu.learning.marketplace.courseStorage.changeHost

import com.intellij.ide.util.PropertiesComponent

private const val COURSE_STORAGE_PRODUCTION_URL = "https://edu-aws-tracks.labs.jb.gg"
private const val COURSE_STORAGE_STAGING_URL = "https://edu-aws-tracks-staging.labs.jb.gg"

enum class CourseStorageServiceHost(private val visibleName: String, val url: String) {
  PRODUCTION("Production server", COURSE_STORAGE_PRODUCTION_URL),
  STAGING("Staging server", COURSE_STORAGE_STAGING_URL),
  OTHER("Other", "http://localhost:8080");

  override fun toString(): String = visibleName

  companion object {
    const val COURSE_STORAGE_SERVICE_HOST_PROPERTY = "course.storage.host"

    @JvmStatic
    fun getSelectedHost(): CourseStorageServiceHost = values().firstOrNull { it.url == getSelectedUrl() } ?: OTHER

    @JvmStatic
    fun getSelectedUrl(defaultUrl: String = PRODUCTION.url): String {
      return PropertiesComponent.getInstance().getValue(COURSE_STORAGE_SERVICE_HOST_PROPERTY, defaultUrl)
    }

    @JvmStatic
    fun setUrl(valueUrl: String, defaultUrl: String) {
      PropertiesComponent.getInstance().setValue(COURSE_STORAGE_SERVICE_HOST_PROPERTY, valueUrl, defaultUrl)
    }
  }
}