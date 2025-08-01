package com.jetbrains.edu.learning.marketplace.courseStorage.api

import com.jetbrains.edu.learning.marketplace.HUB_API_PATH
import com.jetbrains.edu.learning.marketplace.HUB_AUTH_URL

class CourseStorageConnectorImpl : CourseStorageConnector() {
  override val baseUrl: String = "$HUB_AUTH_URL$HUB_API_PATH"
}