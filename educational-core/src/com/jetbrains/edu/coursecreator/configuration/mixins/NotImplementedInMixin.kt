package com.jetbrains.edu.coursecreator.configuration.mixins


internal class NotImplementedInMixin : IllegalStateException("Method from actual class should be called, not from mixin")