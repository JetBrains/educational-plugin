package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.api.Author
import com.jetbrains.edu.learning.marketplace.api.MarketplaceUserInfo

interface UserInfo {
  var isGuest: Boolean

  fun getFullName(): String
}

fun Course.setMarketplaceAuthorsAsString(authors: List<Author>) {
  this.authors = authors.map { MarketplaceUserInfo(it.name) }
}
