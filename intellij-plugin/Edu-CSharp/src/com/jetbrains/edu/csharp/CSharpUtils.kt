package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.rider.ideaInterop.fileTypes.msbuild.CsprojFileType
import com.jetbrains.rider.projectView.projectTemplates.components.ProjectTemplateTargetFramework

val DEFAULT_DOT_NET = ProjectTemplateTargetFramework.latest.presentation

fun Task.csProjPathByTask(project: Project): String = GeneratorUtils.joinPaths(pathByStudyItem(project, this), getCSProjFileName())

fun getDotNetVersion(version: String?): String = version ?: DEFAULT_DOT_NET

fun pathByStudyItem(project: Project, item: StudyItem): String =
  GeneratorUtils.joinPaths(project.basePath, item.pathInCourse)

fun Task.getCSProjFileName(): String = "${getCSProjFileNameWithoutExtension()}.${CsprojFileType.defaultExtension}"

fun Task.getCSProjFileNameWithoutExtension(): String = pathInCourse.formatForCSProj()

fun String.formatForCSProj(): String = split(" ").joinToString("") { it.capitalize() }
  .split("/").joinToString(".") { it.capitalize() }

