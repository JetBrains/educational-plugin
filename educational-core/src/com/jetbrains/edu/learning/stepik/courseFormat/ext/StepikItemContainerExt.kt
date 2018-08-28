@file:JvmName("StepikItemContainerExt")

package com.jetbrains.edu.learning.stepik.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse

fun ItemContainer.getLesson(lessonId: Int): Lesson? {
  return items.filterIsInstance(Lesson::class.java).firstOrNull { item -> item.id == lessonId }
}
