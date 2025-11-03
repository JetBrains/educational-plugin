package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.eventLog.events.PrimitiveEventField

private data class ValidatedByAllowedValuesIntEventField(
  override val name: String,
  private val allowedValues: List<Int>,
) : PrimitiveEventField<Int>() {

  override val validationRule: List<String>
    get() = listOf("{enum:${allowedValues.joinToString("|")}}")

  override fun addData(fuData: FeatureUsageData, value: Int) {
    fuData.addData(name, value)
  }
}

@Suppress("FunctionName", "UnusedReceiverParameter")
fun EventFields.Int(name: String, allowedValues: List<Int>): PrimitiveEventField<Int> {
  return ValidatedByAllowedValuesIntEventField(name, allowedValues)
}
