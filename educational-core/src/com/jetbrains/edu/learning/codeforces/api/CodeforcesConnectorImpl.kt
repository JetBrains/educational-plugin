package com.jetbrains.edu.learning.codeforces.api

import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_URL

class CodeforcesConnectorImpl : CodeforcesConnector() {
  override val baseUrl: String get() = CODEFORCES_URL
}