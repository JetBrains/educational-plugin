package com.jetbrains.edu.learning.taskDescription.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import io.netty.handler.codec.http.HttpRequest
import javax.swing.JComponent

fun isSupported(): Boolean = false

fun getJCEFToolWindow(project: Project): TaskDescriptionToolWindow? = null

fun getJCEFComponent(parentDisposable: Disposable, html: String): JComponent? = null

fun getHostName(httpRequest: HttpRequest): String? = null
