package com.jetbrains.edu.learning.newproject

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.PathUtil
import com.intellij.util.io.IOUtil
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.capitalize
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.ext.languageDisplayName
import java.io.File

fun Course.nameToFileSystemName(): String {
  var fileSystemName = name
  if (!IOUtil.isAscii(fileSystemName)) {
    // there are problems with venv creation for python course
    fileSystemName = "${EduNames.COURSE} $languageDisplayName $humanLanguage".capitalize()
  }
  if (!PathUtil.isValidFileName(fileSystemName)) {
    fileSystemName = FileUtil.sanitizeFileName(fileSystemName)
  }
  return fileSystemName
}

fun Course.nameToLocation(): String {
  return FileUtil.findSequentNonexistentFile(File(ProjectUtil.getBaseDir()), nameToFileSystemName(), "").absolutePath
}
