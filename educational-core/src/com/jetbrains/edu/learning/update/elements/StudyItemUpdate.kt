package com.jetbrains.edu.learning.update.elements

import com.jetbrains.edu.learning.courseFormat.StudyItem

/**
 * This base class is used to store updating information of the `StudyItem` object
 */
sealed class StudyItemUpdate<out T : StudyItem>(open val localItem: T?, open val remoteItem: T?)