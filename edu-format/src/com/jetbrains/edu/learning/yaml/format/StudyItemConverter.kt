package com.jetbrains.edu.learning.yaml.format

import com.fasterxml.jackson.databind.util.StdConverter
import com.jetbrains.edu.learning.courseFormat.StudyItem


class StudyItemConverter : StdConverter<StudyItem, String>() {
  override fun convert(item: StudyItem): String = item.name
}