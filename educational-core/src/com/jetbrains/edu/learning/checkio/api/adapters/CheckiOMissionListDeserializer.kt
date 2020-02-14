package com.jetbrains.edu.learning.checkio.api.adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOMission
import com.jetbrains.edu.learning.checkio.courseFormat.CheckiOStation
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.DescriptionFormat
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseGeneration.GeneratorUtils.convertToValidName
import java.lang.reflect.Type

class CheckiOMissionListDeserializer : JsonDeserializer<List<CheckiOMission>> {
  override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): List<CheckiOMission> {
    return json.asJsonObject["objects"].asJsonArray.map { it.asJsonObject }.map {
      val station = CheckiOStation()
      station.id = it["stationId"].asInt
      station.computeNames(it.get("stationName").asString)

      val mission = CheckiOMission()
      mission.station = station
      mission.id = it["id"].asInt
      mission.computeNames(it.get("title").asString)
      mission.descriptionFormat = DescriptionFormat.HTML
      mission.descriptionText = it["description"].asString
      mission.status = if (it["isSolved"].asBoolean) CheckStatus.Solved else CheckStatus.Unchecked
      mission.code = it["code"].asString
      mission.slug = it["slug"].asString
      val secondsPast = it["secondsPast"]
      // null value means that this mission hasn't been started on CheckiO yet,
      // in this case we should use local task file content after course updating,
      // so time from any local change must be less than from server
      mission.secondsFromLastChangeOnServer = if (secondsPast.isJsonNull) Long.MAX_VALUE else secondsPast.asLong
      mission
    }
  }

  private fun StudyItem.computeNames(originalName: String) {
    name = originalName.convertToValidName()
    if (name != originalName) {
      customPresentableName = originalName
    }
  }
}