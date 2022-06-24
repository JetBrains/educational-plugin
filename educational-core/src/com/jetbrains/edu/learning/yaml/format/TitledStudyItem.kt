package com.jetbrains.edu.learning.yaml.format

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.StudyItem

/**
 * Placeholder for any StudyItem, should be filled with actual content later
 */
class TitledStudyItem(title: String) : StudyItem(title) {

  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    throw NotImplementedError()
  }

  override val course: Course
    get() = throw NotImplementedError()
  override val itemType: String
    get() = throw NotImplementedError()
}