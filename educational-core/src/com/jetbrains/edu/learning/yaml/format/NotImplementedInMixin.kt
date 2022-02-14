package com.jetbrains.edu.learning.yaml.format

import org.jetbrains.annotations.NonNls

@NonNls
private const val ERROR_TO_LOG = "Method from actual class should be called, not from mixin"

internal class NotImplementedInMixin : IllegalStateException(ERROR_TO_LOG)