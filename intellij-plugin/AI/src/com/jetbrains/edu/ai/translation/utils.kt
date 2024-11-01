package com.jetbrains.edu.ai.translation

import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.educational.core.format.domain.MarketplaceId
import com.jetbrains.educational.core.format.domain.TaskEduId
import com.jetbrains.educational.core.format.domain.UpdateVersion

val EduCourse.marketplaceId: MarketplaceId
  get() = MarketplaceId(id)

val EduCourse.updateVersion: UpdateVersion
  get() = UpdateVersion(course.marketplaceCourseVersion)

val Task.taskEduId: TaskEduId
  get() = TaskEduId(id)
