package com.jetbrains.edu.learning

import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironment
import com.jetbrains.edu.learning.newproject.environment.LanguageEnvironmentCatalogProvider

/**
 * TODO EDU-8931 When all course builders implement this interface, it should be merged into the base EduCourseBuilder interface.
 */
interface EnvironmentAwareCourseBuilder<E : LanguageEnvironment> : EduCourseBuilder<E> {
  fun getLanguageEnvironmentCatalogProvider(): LanguageEnvironmentCatalogProvider<E>
}
