@file:JvmName("StepikItemContainerExt")

package com.jetbrains.edu.learning.stepik.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson

fun ItemContainer.getLesson(lessonId: Int): Lesson? {
  return items.filterIsInstance(Lesson::class.java).firstOrNull { item -> item.id == lessonId }
}
