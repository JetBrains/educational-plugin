package com.jetbrains.edu.learning.serialization.converter.json

import com.fasterxml.jackson.databind.node.ObjectNode

interface JsonStepOptionsConverter {
  fun convert(stepOptionsJson: ObjectNode): ObjectNode
}
