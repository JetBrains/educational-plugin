package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.isCLionNova

// TODO: Drop it.
// See: EDU-8805 Drop support of CLion Classic Engine
class LanguageHelperServiceImpl : LanguageHelperService {
  override val cppLanguageId: String
    get() = if (isCLionNova()) {
      "C++"
    }
    else {
      "ObjectiveC"
    }
}