package com.jetbrains.edu.learning.json.migration

class FeedbackLink {
  var type: LinkType = LinkType.STEPIK
  var link: String? = null

  enum class LinkType {
    MARKETPLACE,
    STEPIK,
    CUSTOM,
    NONE
  }
}
