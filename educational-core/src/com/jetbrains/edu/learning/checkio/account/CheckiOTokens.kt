package com.jetbrains.edu.learning.checkio.account

import com.jetbrains.edu.learning.authUtils.TokenInfo

fun TokenInfo.isUpToDate(): Boolean {
  return currentTimeSeconds() < expiresIn - 600
}

private fun currentTimeSeconds(): Long {
  return System.currentTimeMillis() / 1000
}

