package com.jetbrains.edu.sql.jvm.gradle

import com.intellij.database.view.DatabaseViewPanel

// BACKCOMPAT: 2023.2. Inline it.
fun getTree(panel: DatabaseViewPanel) = panel.getTree()