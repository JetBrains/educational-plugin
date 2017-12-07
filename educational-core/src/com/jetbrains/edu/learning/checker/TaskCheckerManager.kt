package com.jetbrains.edu.learning.checker

import com.intellij.lang.LanguageExtensionPoint
import com.intellij.openapi.extensions.Extensions
import com.jetbrains.edu.learning.courseFormat.tasks.Task

object TaskCheckerManager {
    @JvmStatic
    fun getChecker(task: Task): TaskChecker {
        val language = task.lesson.course.languageID
        return Extensions.getExtensions<LanguageExtensionPoint<TaskChecker>>(TaskChecker.EP_NAME, null)
                .find { it -> it.key == language && it.instance.isAccepted(task) }
                ?.instance ?: DefaultTaskChecker()
    }

    @JvmStatic
    fun getCheckers(task: Task): List<TaskChecker> {
        val language = task.lesson.course.languageID
        return Extensions.getExtensions<LanguageExtensionPoint<TaskChecker>>(TaskChecker.EP_NAME, null)
                .filter { it -> it.key == language && it.instance.isAccepted(task) }
                .takeIf { it.isNotEmpty() }
                ?.map { it.instance } ?: listOf(DefaultTaskChecker())
    }
}

class DefaultTaskChecker: TaskChecker() {
    override fun isAccepted(task: Task) = true
}