package com.jetbrains.edu.learning.configuration.attributesEvaluator

/**
 * A path `dir1/dir2/file1.txt` consists of the following path segments:
 * PathSegment(name=dir1, directory=true)
 * PathSegment(name=dir2, directory=true)
 * PathSegment(name=file1.txt, directory=false)
 */
internal data class PathSegment(val name: String, val directory: Boolean) {
  fun check(mustBeDirectory: Boolean, mustBeFile: Boolean): Boolean {
    // return (mustBeDirectory *implies* isDirectory) && (mustBeFile *implies* isFile)
    return (!mustBeDirectory || directory) && (!mustBeFile || !directory)
  }
}