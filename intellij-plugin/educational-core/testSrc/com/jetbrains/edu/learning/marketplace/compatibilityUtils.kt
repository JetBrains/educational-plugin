package com.jetbrains.edu.learning.marketplace

import com.intellij.ui.JBAccountInfoService.JBAData

// BACKCOMPAT: 2024.1. Inline it
fun mockJBAData(): JBAData = JBAData("test_id", "test user name", "test@email.com", null)
