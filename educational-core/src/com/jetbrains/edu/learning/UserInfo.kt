package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.api.MarketplaceUserInfo

interface UserInfo {
  fun getFullName(): String
}

fun Course.setMarketplaceAuthorsAsString(authors: List<String>) {
  setAuthors(authors.map { MarketplaceUserInfo(it) })
}
