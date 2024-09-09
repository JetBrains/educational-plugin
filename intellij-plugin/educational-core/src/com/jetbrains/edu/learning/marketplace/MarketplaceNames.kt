package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.courseFormat.EduFormatNames

const val MARKETPLACE = EduFormatNames.MARKETPLACE
const val LTI = "LTI"
const val JET_BRAINS_ACCOUNT = "JetBrains Account"
const val HUB_AUTH_URL = "https://hub.jetbrains.com"
const val HUB_API_PATH = "/api/rest"
const val PLUGINS_REPOSITORY_URL = "https://plugins.jetbrains.com"
const val PLUGINS_EDU_DEMO = "https://edu-courses.dev.marketplace.intellij.net"
const val PLUGINS_MASTER_DEMO = "https://master.demo.marketplace.intellij.net"
const val MARKETPLACE_PROFILE_PATH = "$PLUGINS_REPOSITORY_URL/author/me"
const val MARKETPLACE_CREATE_VENDOR_PATH = "$PLUGINS_REPOSITORY_URL/vendor/edu/new"
const val JET_BRAINS_ACCOUNT_PROFILE_PATH = "https://account.jetbrains.com/profile-details"
const val LICENSE_URL = "https://creativecommons.org/licenses/by-sa/4.0/"
const val MARKETPLACE_PLUGIN_URL = "$PLUGINS_REPOSITORY_URL/plugin"
const val MARKETPLACE_COURSES_HELP = "${MARKETPLACE_PLUGIN_URL}/10081-jetbrains-academy/docs/courses-at-marketplace.html"
const val JB_VENDOR_NAME = "JetBrains"
const val REVIEWS = "/reviews"
const val SUBMISSIONS_SERVICE_STAGING_URL = "https://educational-service-dev.labs.jb.gg/"
const val SUBMISSIONS_SERVICE_PRODUCTION_URL = "https://educational-service.labs.jb.gg/"
const val SUBMISSIONS_SERVICE_HOST_PROPERTY = "submission.service.host"

fun getOrganizationVendorPath(vendor: String?) =
  if (vendor != null) {
    "https://plugins.jetbrains.com/organizations/${vendor}/edit"
  }
  else {
    "https://plugins.jetbrains.com/author/me/organizations"
  }