package com.jetbrains.edu.learning.framework.impl.migration

import java.io.DataInput
import java.io.DataOutput
import java.io.IOException

interface RecordConverter {
  @Throws(IOException::class)
  fun convert(input: DataInput, output: DataOutput)
}
