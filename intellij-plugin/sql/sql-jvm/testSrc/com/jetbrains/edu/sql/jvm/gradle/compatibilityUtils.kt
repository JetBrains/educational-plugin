package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.view.DatabaseViewPanel
import javax.swing.JTree

// BACKCOMPAT: 2026.1: Inline the body
fun DatabaseViewPanel.getDatabaseTree(): JTree = getUnderlyingDatabaseViewTree()