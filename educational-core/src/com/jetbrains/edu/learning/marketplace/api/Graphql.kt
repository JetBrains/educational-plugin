package com.jetbrains.edu.learning.marketplace.api

import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

class Graphql {

  fun getSearchQuery(): String = GeneratorUtils.getInternalTemplateText("marketplace.qraphql.loadCourses.txt")
}