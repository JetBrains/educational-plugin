package com.jetbrains.edu.learning.framework.impl

import com.intellij.util.io.storage.AbstractStorage
import java.nio.file.Path

abstract class FrameworkStorageBase(storagePath: Path) : AbstractStorage(storagePath, true)
