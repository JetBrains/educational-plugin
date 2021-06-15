package com.jetbrains.edu.learning.marketplace.api


import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils

object GraphqlQuery {
  const val LOADING_STEP = 10

  fun search(offset: Int, searchPrivate: Boolean): String {
    val templateName = if (searchPrivate) {
      "marketplace.qraphql.loadPrivateCourses.txt"
    }
    else {
      "marketplace.qraphql.loadPublicCourses.txt"
    }
    return GeneratorUtils.getInternalTemplateText(templateName, mapOf<String, Any>("max" to LOADING_STEP, "offset" to offset))
  }

  fun searchById(courseId: Int, searchPrivate: Boolean) =
    if (searchPrivate) {
      GeneratorUtils.getInternalTemplateText("marketplace.qraphql.loadPrivateCourseById.txt",
                                             mapOf("courseId" to courseId))
    }
    else {
      GeneratorUtils.getInternalTemplateText("marketplace.qraphql.loadPublicCourseById.txt",
                                             mapOf("courseId" to courseId))
    }

  /**
   * this query returns the List<UpdateBean>, which should contain a single value - latest update id bean, because in
   * marketplace.qraphql.courseUpdateId.txt.ft  parameter max is set to 1
   */
  fun lastUpdateId(courseId: Int) = GeneratorUtils.getInternalTemplateText("marketplace.qraphql.courseUpdateId.txt",
                                                                           mapOf("courseId" to courseId))

}