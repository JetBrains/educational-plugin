package com.jetbrains.edu.learning.yaml

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.learning.courseDir
import com.jetbrains.edu.learning.courseFormat.FrameworkLesson
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.getDir
import com.jetbrains.edu.learning.courseFormat.ext.getTaskDirectory
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.configFileName
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.remoteConfigFileName

/**
 * Returns the directory where yaml config files are stored.
 *
 * In most cases it's the same as [getDir] except the case when it's [Task] inside [FrameworkLesson] in student mode.
 * In this case, all meta files, including config ones, are stored separately
 */
fun StudyItem.getConfigDir(project: Project): VirtualFile {
  val configDir = if (this is Task) getTaskDirectory(project) else getDir(project.courseDir)
  return configDir ?: error("Config dir for `$name` not found")
}

fun StudyItem.configFile(project: Project): VirtualFile? {
  return getConfigDir(project).findChild(configFileName)
}

fun StudyItem.remoteConfigFile(project: Project): VirtualFile? {
  return getConfigDir(project).findChild(remoteConfigFileName)
}
