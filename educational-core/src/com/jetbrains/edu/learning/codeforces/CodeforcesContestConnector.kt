package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_URL
import org.jsoup.nodes.Document

object CodeforcesContestConnector {
  fun getContestIdFromLink(link: String): Int {
    val match = Regex("codeforces.com/contest/(\\d*)").find(link) ?: return -1
    return match.destructured.component1().also { if (it == "") return -1 }.toInt()
  }

  fun getContestURLFromID(id: Int): String = "$CODEFORCES_URL/contest/$id"

  fun getContestId(text: String): Int =
    try {
      Integer.parseInt(text)
    }
    catch (e: NumberFormatException) {
      getContestIdFromLink(text)
    }

  fun getLanguages(contest: Document): List<String>? {
    val supportedLanguages = CodeforcesLanguageProvider.getSupportedLanguages()
    return contest.selectFirst("#programTypeForInvoker")
      ?.select("option")
      ?.map { it.text() }
      ?.filter { language ->
        language in supportedLanguages
      }
  }
}