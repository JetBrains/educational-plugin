package com.jetbrains.edu.learning.codeforces

import com.jetbrains.edu.learning.messages.EduCoreBundle

// TODO: move to message bundle
object CodeforcesNames {
  const val CODEFORCES: String = "codeforces"
  const val CODEFORCES_TITLE: String = "Codeforces"
  const val CODEFORCES_URL: String = "https://codeforces.com"
  const val CODEFORCES_SUBMIT: String = "submit"
  const val CONTEST: String = "contest"
  const val CODEFORCES_PROBLEMS: String = "Problems"
  const val CODEFORCES_HELP_TEXT: String = "https://codeforces.com/contest/*"
  const val TEST_DATA_FOLDER: String = "testData"
  const val CODEFORCES_CONTEST_SUBMISSIONS_URL: String = "$CODEFORCES_URL/contest/%d/my"
  const val CODEFORCES_EDU_TOOLS_HELP = "https://www.jetbrains.com/help/education/codeforces-contests.html"

  private const val CONTESTS_POST = "http://codeforces.com/blog/entry/456"
  private const val CONTESTS_RULES = "http://codeforces.com/blog/entry/4088"
  private const val THIRD_PARTY_CODE_RULE_CHANGING = "http://codeforces.com/blog/entry/8790"
  val TERMS_OF_AGREEMENT = EduCoreBundle.message("codeforces.default.terms.of.agreement", CONTESTS_POST, CONTESTS_RULES, THIRD_PARTY_CODE_RULE_CHANGING)
  const val DEFAULT_TERMS_OF_AGREEMENT = "The registration confirms that you:\n" +
                     "* have read the contest rules\n" +
                     "* will not violate the rules\n" +
                     "* will not communicate with other participants, use another person's code for solutions/generators, share ideas of solutions and hacks\n" +
                     "* will not attempt to deliberately destabilize the testing process and try to hack the contest system in any form\n" +
                     "* will not use multiple accounts and will take part in the contest using your personal and the single account."

  const val CODEFORCES_TASK_TYPE: String = CODEFORCES
  const val CODEFORCES_TASK_TYPE_WITH_FILE_IO: String = "${CODEFORCES}_file_io"
  val CODEFORCES_COURSE_TYPE: String = CODEFORCES.capitalize()
}