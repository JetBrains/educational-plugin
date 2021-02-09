package com.jetbrains.edu.learning.framework.impl

import com.intellij.util.io.PagePool
import com.intellij.util.io.storage.AbstractRecordsTable
import java.nio.file.Path

abstract class FrameworkRecordsTableBase(storageFilePath: Path, pool: PagePool) : AbstractRecordsTable(storageFilePath, pool)
