package com.jetbrains.edu.learning

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

fun <T1, T2> CoroutineScope.combineStateFlow(
  state1: StateFlow<T1>,
  state2: StateFlow<T2>,
): StateFlow<Pair<T1, T2>> = combine(state1, state2) { t1, t2 -> t1 to t2 }
  .stateIn(this, SharingStarted.Eagerly, state1.value to state2.value)
