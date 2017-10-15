package com.jetbrains.edu.learning.languageColors

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorDeserializationTest {

  private val gson: Gson = Gson()

  @Test
  fun deserialize() {
    val color = gson.fromJson("""
      {
        "color": "#F18E33",
        "url": "https://github.com/trending?l=Kotlin"
      }
    """, LanguageColor::class.java)
    assertEquals(LanguageColor("#F18E33"), color)
  }

  @Test
  fun `deserialize color map`() {
    val colors = gson.fromJson<Map<String, LanguageColor>>("""
      {
        "Rust": {
          "color": "#dea584",
          "url": "https://github.com/trending?l=Rust"
        },
        "Kotlin": {
          "color": "#F18E33",
          "url": "https://github.com/trending?l=Kotlin"
        }
      }
      """, object : TypeToken<Map<String, LanguageColor>>(){}.type)

    assertEquals(2, colors.size)
    assertEquals(LanguageColor("#dea584"), colors["Rust"])
    assertEquals(LanguageColor("#F18E33"), colors["Kotlin"])
  }
}
