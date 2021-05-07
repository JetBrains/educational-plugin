package com.jetbrains.edu.learning.codeforces

import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.codeforces.CodeforcesNames.CODEFORCES_URL
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import org.jsoup.select.Elements
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.*

object CodeforcesContestConnector {
  private const val TD_TAG = "td"
  private const val TR_TAG = "tr"
  private const val DATA_CONTEST_ID_ATTR = "data-contestId"
  private const val FORMAT_DATE_CLASS = "format-date"
  private const val FORMAT_TIME_CLASS = "format-time"
  private const val DATATABLE_CLASS = "datatable"
  private const val CONTEST_LIST_CLASS = "contestList"
  private const val CONTEST_TABLE_CLASS = "contests-table"

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

  fun getUpcomingContests(document: Document): List<ContestInformation> {
    val upcomingContestList = document.body().getElementsByClass(CONTEST_LIST_CLASS).first() ?: error("Cannot parse $CONTEST_LIST_CLASS")
    val tables = upcomingContestList.getElementsByClass(DATATABLE_CLASS)
    if (tables.isEmpty()) {
      return emptyList()
    }
    val upcomingContests = tables[0]

    val contestElements = getContestsElements(upcomingContests)

    return contestElements.filter { it.attr(DATA_CONTEST_ID_ATTR).isNotEmpty() }.mapNotNull {
      parseContestInformation(it, FORMAT_TIME_CLASS)
    }
  }

  /**
   * Recent contest table goes after upcoming and current contests table.
   * See the page format [com.jetbrains.edu.learning.codeforces.api.CodeforcesService.contestsPage]
   */
  fun getRecentContests(document: Document): List<ContestInformation> {
    val recentContestsParent = document.body().getElementsByClass(CONTEST_TABLE_CLASS).first() ?: return emptyList()
    val recentContests = recentContestsParent.getElementsByClass(DATATABLE_CLASS)?.first() ?: return emptyList()

    val contestElements = getContestsElements(recentContests)

    return contestElements.filter { it.attr(DATA_CONTEST_ID_ATTR).isNotEmpty() }.mapNotNull {
      parseContestInformation(it, FORMAT_DATE_CLASS)
    }
  }

  private fun parseContestInformation(element: Element, dateClass: String): ContestInformation? {
    return try {
      val contestId = element.attr(DATA_CONTEST_ID_ATTR).toInt()
      val tableRow = element.getElementsByTag(TD_TAG)

      val contestName = (tableRow[0].childNodes().first() as TextNode).text().trim()
      val startDate = parseDate(tableRow, dateClass)
      val contestLength = tableRow[3].text().split(":").map { it.toLong() }
      val isRegistrationOpen = tableRow[5].getElementsByClass("countdown").isNotEmpty()
      val duration = Duration.ofHours(contestLength[0]).plus(Duration.ofMinutes(contestLength[1]))
      ContestInformation(contestId, contestName, startDate + duration, startDate, duration, isRegistrationOpen)
    }
    catch (e: Exception) {
      Logger.getInstance(this::class.java).warn(e.message)
      null
    }
  }

  private fun parseDate(tableRow: Elements, dateClass: String): ZonedDateTime {
    val dateElement = tableRow[2].getElementsByClass(dateClass).first()
    val dateLocaleString = tableRow[2].getElementsByClass(dateClass).attr("data-locale")
    val dateLocale = Locale.Builder().setLanguage(dateLocaleString).build()
    val startDateString = dateElement.text()
    val formatter = DateTimeFormatter.ofPattern("MMM/dd/yyyy HH:mm", dateLocale)
    val startDateLocal = LocalDateTime.parse(startDateString, formatter)
    return ZonedDateTime.ofInstant(startDateLocal, OffsetDateTime.now().offset, ZoneId.systemDefault())
  }

  private fun getContestsElements(recentContests: Element) = recentContests.getElementsByTag(TR_TAG)
}