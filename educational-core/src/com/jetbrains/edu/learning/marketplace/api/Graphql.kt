package com.jetbrains.edu.learning.marketplace.api

import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

class Graphql {

  fun getSearchQuery(offset: Int): String = GeneratorUtils.getInternalTemplateText("marketplace.qraphql.loadCourses.txt",
                                                                                   mapOf("max" to LOADING_STEP, "offset" to offset))

  companion object {
    const val LOADING_STEP = 10
  }
}