package com.jetbrains.edu.learning.stepik

import com.google.common.annotations.VisibleForTesting
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.allTasks
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.stepik.api.Reply
import com.jetbrains.edu.learning.stepik.api.StepikConnector
import com.jetbrains.edu.learning.stepik.api.Submission
import java.util.concurrent.ConcurrentHashMap

abstract class SubmissionsManager {
  val submissions = ConcurrentHashMap<Int, MutableList<Submission>>()

  abstract fun getSubmissionsFromMemory(taskId: Int): List<Submission>?

  @VisibleForTesting
  fun clear() {
    submissions.clear()
  }
}