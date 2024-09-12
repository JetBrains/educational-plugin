package com.jetbrains.edu.csharp

import com.intellij.openapi.project.Project
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils
import com.jetbrains.rider.ideaInterop.fileTypes.msbuild.CsprojFileType
import com.jetbrains.rider.projectView.projectTemplates.components.ProjectTemplateTargetFramework
import com.jetbrains.rider.projectView.workspace.ProjectModelEntity
import com.jetbrains.rider.projectView.workspace.findProjectsByName
import com.jetbrains.rider.projectView.workspace.getSolutionEntity

val DEFAULT_DOT_NET = ProjectTemplateTargetFramework.latest.presentation

fun Task.csProjPathByTask(project: Project): String = GeneratorUtils.joinPaths(getDir(project.courseDir)?.path, getCSProjFileName())

fun getDotNetVersion(version: String?): String = version ?: DEFAULT_DOT_NET

fun Task.getCSProjFileName(): String = "${getCSProjFileNameWithoutExtension()}.${CsprojFileType.defaultExtension}"

fun Task.getCSProjFileNameWithoutExtension(): String = pathInCourse.formatForCSProj()

fun String.formatForCSProj(): String = split("/").joinToString(".") { it.capitalize() }

fun Task.getTestName(): String = getCSProjFileNameWithoutExtension().toTestName()

fun String.toTestName(): String = filter { it != '.' } + "Test"
fun Task.toProjectModelEntity(project: Project): ProjectModelEntity? = WorkspaceModel.getInstance(project).findProjectsByName(getCSProjFileNameWithoutExtension()).firstOrNull()

fun Project.getSolutionEntity(): ProjectModelEntity? = WorkspaceModel.getInstance(this).getSolutionEntity()