package com.jetbrains.edu.learning.stepik

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.submissions.Submission

class StepikCheckListener: PostSolutionCheckListener() {

  override fun postSubmission(project: Project, task: Task): Submission? = StepikSolutionsLoader.postSolution(task,
                                                                                                              task.status == CheckStatus.Solved,
                                                                                                              project)

  override fun isUpToDate(course: EduCourse, task: Task): Boolean = task.isUpToDate

  override fun updateCourseAction(project: Project, course: EduCourse) = updateCourseOnStepik(project, course)

  override fun EduCourse.isToPostSubmissions(): Boolean = isStepikRemote && EduSettings.isLoggedIn()
}