package com.jetbrains.edu.learning.codeforces.api

import com.jetbrains.edu.learning.courseFormat.EduFormatNames.CODEFORCES_URL

class CodeforcesConnectorImpl : CodeforcesConnector() {
  override val baseUrl: String get() = CODEFORCES_URL
}