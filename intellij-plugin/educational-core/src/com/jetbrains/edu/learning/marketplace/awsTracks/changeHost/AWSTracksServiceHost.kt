package com.jetbrains.edu.learning.marketplace.awsTracks.changeHost

import com.intellij.ide.util.PropertiesComponent

private const val AWS_TRACKS_SERVICE_PRODUCTION_URL = "https://edu-aws-tracks.labs.jb.gg"
private const val AWS_TRACKS_SERVICE_STAGING_URL = "https://edu-aws-tracks-staging.labs.jb.gg"

enum class AWSTracksServiceHost(private val visibleName: String, val url: String) {
  PRODUCTION("Production server", AWS_TRACKS_SERVICE_PRODUCTION_URL),
  STAGING("Staging server", AWS_TRACKS_SERVICE_STAGING_URL),
  OTHER("Other", "http://localhost:8080");

  override fun toString(): String = visibleName

  companion object {
    const val AWS_TRACKS_SERVICE_HOST_PROPERTY = "aws.tracks.service.host"

    @JvmStatic
    fun getSelectedHost(): AWSTracksServiceHost = values().firstOrNull { it.url == getSelectedUrl() } ?: OTHER

    @JvmStatic
    fun getSelectedUrl(defaultUrl: String = PRODUCTION.url): String {
      return PropertiesComponent.getInstance().getValue(AWS_TRACKS_SERVICE_HOST_PROPERTY, defaultUrl)
    }

    @JvmStatic
    fun setUrl(valueUrl: String, defaultUrl: String) {
      PropertiesComponent.getInstance().setValue(AWS_TRACKS_SERVICE_HOST_PROPERTY, valueUrl, defaultUrl)
    }
  }
}