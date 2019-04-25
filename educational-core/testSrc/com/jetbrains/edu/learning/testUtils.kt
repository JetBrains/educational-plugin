package com.jetbrains.edu.learning

import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher

inline fun <reified T> nullValue(): Matcher<T> = CoreMatchers.nullValue(T::class.java)
