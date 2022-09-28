package com.jetbrains.edu.learning.json.mixins

import org.jetbrains.annotations.NonNls

@NonNls
private const val ERROR_TO_LOG = "Method from actual class should be called, not from mixin"

class NotImplementedInMixin : IllegalStateException(ERROR_TO_LOG)