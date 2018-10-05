package com.jetbrains.edu.learning.checkio.account

import com.jetbrains.edu.learning.TokenInfo

fun TokenInfo.isUpToDate(): Boolean {
  return currentTimeSeconds() < expiresIn - 600
}

private fun currentTimeSeconds(): Long {
  return System.currentTimeMillis() / 1000
}

