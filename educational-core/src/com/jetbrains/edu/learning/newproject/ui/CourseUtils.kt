package com.jetbrains.edu.learning.newproject.ui

import com.jetbrains.edu.learning.core.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import java.util.*

internal object CourseUtils {

    @JvmStatic
    fun getAuthorFullNames(course: Course): List<String> = course.authors.map { user -> "${user.firstName} ${user.lastName}" }

    @JvmStatic
    fun getTags(course: Course): List<String> {
        val tags = ArrayList<String>()
        val language = course.languageById
        if (language != null) {
            tags.add(language.displayName)
        }
        if (course.isAdaptive) {
            tags.add(EduNames.ADAPTIVE)
        }
        return tags
    }
}
