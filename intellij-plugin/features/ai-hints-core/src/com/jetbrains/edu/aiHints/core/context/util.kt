package com.jetbrains.edu.aiHints.core.context

import com.jetbrains.edu.aiHints.core.context.TaskFileHintsDataHolder.Companion.hintData
import com.jetbrains.edu.learning.courseFormat.TaskFile

/**
 * Represents function signatures used in the task file.
 */
var TaskFile.functionSignatures: List<FunctionSignature>?
  get() = hintData.functionSignatures.value
  set(value) {
    hintData.functionSignatures.value = value
  }

/**
 * Represents the snapshot hash of the task file since the last time the function signatures in the file were updated.
 */
var TaskFile.functionSignaturesSnapshotHash: Int?
  get() = hintData.functionSignatures.snapshotHash
  set(value) {
    hintData.functionSignatures.snapshotHash = value
  }

/**
 * Represents the hash of the task file content at the last snapshot.
 * The snapshot file hash is used to determine whether a file has been changed or not.
 */
var TaskFile.snapshotFileHash: Int?
  get() = hintData.snapshotFileHash
  set(value) {
    hintData.snapshotFileHash = value
  }

/**
 * Represents a list of strings that have been used in the task file.
 */
var TaskFile.usedStrings: List<String>?
  get() = hintData.usedStrings.value
  set(value) {
    hintData.usedStrings.value = value
  }

/**
 * Represents the snapshot hash of the task file since the last time the used strings in the file were updated.
 */
var TaskFile.usedStringsSnapshotHash: Int?
  get() = hintData.usedStrings.snapshotHash
  set(value) {
    hintData.usedStrings.snapshotHash = value
  }