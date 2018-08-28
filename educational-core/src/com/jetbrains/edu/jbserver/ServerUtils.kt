package com.jetbrains.edu.jbserver

import com.jetbrains.edu.learning.courseFormat.StepikChangeStatus
import com.jetbrains.edu.learning.courseFormat.StudyItem


fun StudyItem.isChanged()
  = stepikChangeStatus != StepikChangeStatus.UP_TO_DATE


fun <T> MutableList<T>.mapInplace (mutator: (T) -> T) {
  val iterator = this.listIterator()
  while (iterator.hasNext()) {
    val oldValue = iterator.next()
    val newValue = mutator(oldValue)
    iterator.set(newValue)
  }
}
