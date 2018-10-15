@file:JvmName("StepikStudyItemExt")

package com.jetbrains.edu.learning.stepik.courseFormat.ext

import com.jetbrains.edu.learning.courseFormat.ItemContainer
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.Section
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.stepik.courseFormat.StepikCourse

val StudyItem.id: Int get() = (this as? StepikCourse)?.id ?: (this as? Section)?.id ?: (this as? Lesson)?.id ?: (this as? Task)?.stepId ?: 0

fun ItemContainer.getLesson(lessonId: Int): Lesson? {
  return items.asSequence().filterIsInstance(Lesson::class.java).firstOrNull { item -> item.id == lessonId }
}
