package com.jetbrains.edu.learning.serialization.converter.json;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;

import static com.jetbrains.edu.learning.serialization.SerializationUtils.Json.*;
import static com.jetbrains.edu.learning.serialization.SerializationUtils.*;

public class ToSecondVersionJsonStepOptionsConverter implements JsonStepOptionsConverter {

  @NotNull
  @Override
  public JsonObject convert(@NotNull JsonObject stepOptionsJson) {
    Gson gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    final JsonArray files = stepOptionsJson.getAsJsonArray(FILES);
    if (files != null) {
      for (JsonElement taskFileElement : files) {
        JsonObject taskFileObject = taskFileElement.getAsJsonObject();
        JsonArray placeholders = taskFileObject.getAsJsonArray(PLACEHOLDERS);
        for (JsonElement placeholder : placeholders) {
          JsonObject placeholderObject = placeholder.getAsJsonObject();
          convertToAbsoluteOffset(taskFileObject, placeholderObject);
          convertMultipleHints(gson, placeholderObject);
          convertToSubtaskInfo(placeholderObject);
        }
      }
    }
    return stepOptionsJson;
  }

  private static void convertToAbsoluteOffset(@NotNull JsonObject taskFileObject, @NotNull JsonObject placeholderObject) {
    int line = placeholderObject.getAsJsonPrimitive(LINE).getAsInt();
    int start = placeholderObject.getAsJsonPrimitive(START).getAsInt();
    if (line == -1) {
      placeholderObject.addProperty(OFFSET, start);
    }
    else {
      Document document = EditorFactory.getInstance().createDocument(taskFileObject.getAsJsonPrimitive(TEXT).getAsString());
      placeholderObject.addProperty(OFFSET, document.getLineStartOffset(line) + start);
    }
  }

  private static void convertMultipleHints(@NotNull Gson gson, @NotNull JsonObject placeholderObject) {
    final String hintString = placeholderObject.getAsJsonPrimitive(HINT).getAsString();
    final JsonArray hintsArray = new JsonArray();

    try {
      final Type listType = new TypeToken<List<String>>() {
      }.getType();
      final List<String> hints = gson.fromJson(hintString, listType);
      if (hints != null && !hints.isEmpty()) {
        for (int i = 0; i < hints.size(); i++) {
          if (i == 0) {
            placeholderObject.addProperty(HINT, hints.get(0));
            continue;
          }
          hintsArray.add(hints.get(i));
        }
        placeholderObject.add(ADDITIONAL_HINTS, hintsArray);
      }
      else {
        placeholderObject.addProperty(HINT, "");
      }
    }
    catch (JsonParseException e) {
      hintsArray.add(hintString);
    }
  }

  private static void convertToSubtaskInfo(@NotNull JsonObject placeholderObject) {
    JsonArray subtaskInfos = new JsonArray();
    placeholderObject.add(SUBTASK_INFOS, subtaskInfos);
    JsonArray hintsArray = new JsonArray();
    hintsArray.add(placeholderObject.getAsJsonPrimitive(HINT).getAsString());
    JsonArray additionalHints = placeholderObject.getAsJsonArray(ADDITIONAL_HINTS);
    if (additionalHints != null) {
      hintsArray.addAll(additionalHints);
    }
    JsonObject subtaskInfo = new JsonObject();
    subtaskInfos.add(subtaskInfo);
    subtaskInfo.add(INDEX, new JsonPrimitive(0));
    subtaskInfo.add(HINTS, hintsArray);
    subtaskInfo.addProperty(POSSIBLE_ANSWER, placeholderObject.getAsJsonPrimitive(POSSIBLE_ANSWER).getAsString());
  }
}
