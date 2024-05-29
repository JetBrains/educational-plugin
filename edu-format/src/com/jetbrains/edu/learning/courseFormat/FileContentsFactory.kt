package com.jetbrains.edu.learning.courseFormat

const val FILE_CONTENTS_FACTORY_INJECTABLE_VALUE = "injected file contents factory"

/**
 * This factory is used to create file contents during deserialization of [EduFile], [TaskFile].
 * It is used when json or yaml do not have the contents themselves, i.e., when the `text` field is empty.
 * In this case, we need to make file contents point somewhere outside the deserialized JSON or YAML.
 *
 * When we deserialize YAML, we need to point to the learning objects storage.
 * When we deserialize JSON from a `course.zip` file, we need to point to some file inside `course.zip`.
 */
interface FileContentsFactory {
  /**
   * [file] is the [EduFile] object, that will contain this contents.
   * This file may not be fully initialized at the moment of the call.
   * For example, if it is a [TaskFile] it probably does not have a [TaskFile.task] field initialized.
   *
   * But when the file contents are used, i.e., its bytes are retrieved, the file is guaranteed to be initialized, so
   * it becomes possible to get its path relative to the course directory.
   */
  fun createBinaryContents(file: EduFile): BinaryContents

  /**
   * see [createBinaryContents]
   */
  fun createTextualContents(file: EduFile): TextualContents
}
