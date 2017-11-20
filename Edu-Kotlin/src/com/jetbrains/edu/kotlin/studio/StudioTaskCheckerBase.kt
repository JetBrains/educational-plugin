package com.jetbrains.edu.kotlin.studio

import com.jetbrains.edu.learning.EduUtils
import com.jetbrains.edu.learning.checker.TaskChecker
import com.jetbrains.edu.learning.courseFormat.tasks.Task

open class StudioTaskCheckerBase : TaskChecker() {
    override fun isAccepted(task: Task) = EduUtils.isAndroidStudio()
}