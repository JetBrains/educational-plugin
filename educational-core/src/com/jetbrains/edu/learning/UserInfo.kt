package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.marketplace.api.MarketplaceUserInfo
import com.jetbrains.edu.learning.stepik.StepikUserInfo
import java.util.*

interface UserInfo {
  fun getFullName(): String
}

fun Course.setStepikAuthorsAsString(authors: Array<String>) {
  this.authors = ArrayList()
  for (name in authors) {
    this.authors.add(StepikUserInfo(name))
  }
}

fun Course.setMarketplaceAuthorsAsString(authors: List<String>) {
  this.authors = ArrayList()
  for (name in authors) {
    this.authors.add(MarketplaceUserInfo(name))
  }
}
