package com.jetbrains.edu.learning.projectView

import com.intellij.ide.projectView.impl.AbstractProjectViewPaneWithAsyncSupport
import com.intellij.openapi.project.Project

// BACKCOMPACT: 2022.2. Inline it
abstract class CourseViewPaneBase(project: Project) : AbstractProjectViewPaneWithAsyncSupport(project)

