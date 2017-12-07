package com.jetbrains.edu.learning.serialization.converter.json;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.jetbrains.edu.learning.EduNames;
import com.jetbrains.edu.learning.stepik.StepikNames;
import org.jetbrains.annotations.NotNull;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.Json.TITLE;

public class ToFourthVersionJsonStepOptionsConverter implements JsonStepOptionsConverter {

  @NotNull
  @Override
  public JsonObject convert(@NotNull JsonObject stepOptionsJson) {
    if (stepOptionsJson.has(TITLE) &&
        StepikNames.PYCHARM_ADDITIONAL.equals(stepOptionsJson.get(TITLE).getAsString())) {
      stepOptionsJson.remove(TITLE);
      stepOptionsJson.add(TITLE, new JsonPrimitive(EduNames.ADDITIONAL_MATERIALS));
    }
    return stepOptionsJson;
  }
}
