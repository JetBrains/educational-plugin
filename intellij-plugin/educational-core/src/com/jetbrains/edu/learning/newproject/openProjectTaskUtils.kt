package com.jetbrains.edu.learning.newproject

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.OpenProjectTaskBuilder

/**
 * Non-inline version of the top-level [OpenProjectTask] function.
 *
 * The main purpose is to avoid runtime compatibility errors caused by frequent changes of the primary `OpenProjectTask` constructor.
 * Both [OpenProjectTask] and [OpenProjectTaskBuilder.build] are inline kotlin functions,
 * so all changes in [OpenProjectTask] constructor will be inlined into caller code.
 * And any change in it on the platform side will break binary compatibility with the already compiled plugin.
 *
 * Under the hood, it uses Java code to avoid inlining by Kotlin compiler and avoiding error by calling platform code marked with internal modifier.
 * Of course, it's a hack, but it's better than a broken plugin.
 */
fun openProjectTask(buildAction: OpenProjectTaskBuilder.() -> Unit): OpenProjectTask {
  return OpenProjectTaskUtils.createOpenProjectTask(buildAction)
}
