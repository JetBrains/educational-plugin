package com.jetbrains.edu.learning.codeforces

data class ContestURLInfo(
  val id: Int,
  val locale: String,
  val languageId: String
) {
  val url: String by lazy { CodeforcesContestConnector.getContestURLFromID(id) }
}
