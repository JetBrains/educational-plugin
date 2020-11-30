package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount

private const val DATA_DELIMITER = ";"
private const val DELIMITER = "."

fun decodeHubToken(token: String): String? {
  val parts = token.split(DATA_DELIMITER)
  if (parts.size != 2) {
    error("Hub oauth token data part is malformed")
  }
  val userData = parts[0].split(DELIMITER)
  if (userData.size != 4) {
    error("Hub oauth token data part is malformed")
  }
  return if (userData[2].isEmpty()) null else userData[2]
}

val MarketplaceAccount.profileUrl: String get() = "$HUB_PROFILE_PATH${userInfo.id}"