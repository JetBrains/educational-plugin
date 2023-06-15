package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.dataSource.DataSourceStorageCore
import com.intellij.util.messages.Topic

typealias DataSourceStorageListener = DataSourceStorageCore.Listener
val DATA_SOURCE_STORAGE_TOPIC: Topic<DataSourceStorageListener> = DataSourceStorageCore.TOPIC
