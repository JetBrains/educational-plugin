package com.jetbrains.edu.learning.marketplace

import com.jetbrains.edu.learning.marketplace.api.MarketplaceAccount

val MarketplaceAccount.profileUrl: String get() = "$HUB_PROFILE_PATH${userInfo.id}"