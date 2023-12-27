package com.jetbrains.edu.learning.storage

import com.jetbrains.edu.learning.courseFormat.BinaryContents
import com.jetbrains.edu.learning.courseFormat.TextualContents
import com.jetbrains.edu.learning.courseFormat.UndeterminedContents

interface ContentsFromLearningObjectsStorage {
  val storage: LearningObjectsStorage
  val path: String
}

class TextualContentsFromLearningObjectsStorage(
  override val storage: LearningObjectsStorage,
  override val path: String
) : TextualContents, ContentsFromLearningObjectsStorage {
  override val text: String
    get() = String(storage.load(path))
}

class BinaryContentsFromLearningObjectsStorage(
  override val storage: LearningObjectsStorage,
  override val path: String
) : BinaryContents, ContentsFromLearningObjectsStorage {
  override val bytes: ByteArray
    get() = storage.load(path)
}

class UndeterminedContentsFromLearningObjectsStorage(
  override val storage: LearningObjectsStorage,
  override val path: String
) : UndeterminedContents, ContentsFromLearningObjectsStorage {
  override val textualRepresentation: String
    get() = String(storage.load(path))
}