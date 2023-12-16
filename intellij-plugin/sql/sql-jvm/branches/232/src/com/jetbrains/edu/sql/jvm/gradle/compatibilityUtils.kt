package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.DataSourceStorage
import com.intellij.util.messages.Topic

// BACKCOMPAT: 2023.1 Inline
typealias DataSourceStorageListener = DataSourceStorage.Listener
val DATA_SOURCE_STORAGE_TOPIC: Topic<DataSourceStorageListener> = DataSourceStorage.TOPIC
