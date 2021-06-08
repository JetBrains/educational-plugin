package com.jetbrains.edu.learning.marketplace.api


import com.jetbrains.edu.learning.EduExperimentalFeatures
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.edu.learning.isFeatureEnabled

object GraphqlQuery {
  const val LOADING_STEP = 10

  fun search(offset: Int): String {
    val templateName = if (isFeatureEnabled(EduExperimentalFeatures.MARKETPLACE_PRIVATE_COURSES)) {
      "marketplace.qraphql.loadPublicAndPrivateCourses.txt"
    }
    else {
      "marketplace.qraphql.loadPublicCourses.txt"
    }
    return GeneratorUtils.getInternalTemplateText(templateName, mapOf<String, Any>("max" to LOADING_STEP, "offset" to offset))
  }

  fun searchById(courseId: Int) = GeneratorUtils.getInternalTemplateText("marketplace.qraphql.loadCourseById.txt",
                                                                         mapOf("courseId" to courseId))

  /**
   * this query returns the List<UpdateBean>, which should contain a single value - latest update id bean, because in
   * marketplace.qraphql.courseUpdateId.txt.ft  parameter max is set to 1
   */
  fun lastUpdateId(courseId: Int) = GeneratorUtils.getInternalTemplateText("marketplace.qraphql.courseUpdateId.txt",
                                                                           mapOf("courseId" to courseId))

}