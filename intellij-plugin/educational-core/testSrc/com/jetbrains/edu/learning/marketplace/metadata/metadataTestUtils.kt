package com.jetbrains.edu.learning.marketplace.metadata

fun getRandomTrustedUrl(): String = "https://${TRUSTED_METADATA_HOSTS.keys.random()}"