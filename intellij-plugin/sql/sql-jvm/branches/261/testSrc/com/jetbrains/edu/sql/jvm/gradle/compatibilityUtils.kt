package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.view.DatabaseViewPanel
import javax.swing.JTree

fun DatabaseViewPanel.getDatabaseTree(): JTree = getTree()