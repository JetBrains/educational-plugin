package com.jetbrains.edu.learning.stepik

import com.jetbrains.edu.learning.stepik.StepikTaskBuilder.Companion.prepareSample
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class PrepareSampleHtmlTest(private val input: String, private val expected: String) {
  // EDU-4363 Sample input XML is not correctly displayed in the task description
  // EDU-4580 Input with `<` and `>` is not correctly displayed
  @Test
  fun test() {
    val actual = input.prepareSample()
    assertEquals(expected, actual)
  }

  companion object {
    @Parameterized.Parameters
    @JvmStatic
    fun data(): Collection<Array<Any>> = listOf(
      arrayOf("""<a attr="123"><b>hello</b><c/></a>""",
              "&lt;a attr=&quot;123&quot;&gt;&lt;b&gt;hello&lt;/b&gt;&lt;c/&gt;&lt;/a&gt;"),
      arrayOf("""
              |1
              |2
              |3
              |4
              |0
              """.trimMargin(), "1<br>2<br>3<br>4<br>0"),
      arrayOf("""<input value = ">">""", "&lt;input value = &quot;&gt;&quot;&gt;"),
      arrayOf("<br/>", "&lt;br/&gt;"),
      arrayOf("br/>", "br/&gt;"),
      arrayOf("<>", "&lt;&gt;"),
      arrayOf("""
              ,name,age,education,sex,income
              0,254343,36,1,1,>50k
              1,476429,33,7,1,<50k
              2,363510,33,1,0,>50k
              """.trimIndent(),
              ",name,age,education,sex,income<br>0,254343,36,1,1,&gt;50k<br>1,476429,33,7,1,&lt;50k<br>2,363510,33,1,0,&gt;50k"),
      // in this case we must escape html tags and `>` is escaped as well, but it'll be shown the same as without escaping
      arrayOf(",<b>name</b>,age,education,sex,income\n0,254343,36,1,1,>50k",
              """,&lt;b&gt;name&lt;/b&gt;,age,education,sex,income<br>0,254343,36,1,1,&gt;50k"""),
      arrayOf(",name,age,education,sex,income\n0,254343,36,1,1,>50k\n1,476429,33,7,1,<50k\n2,363510,33,1,0,>50k\n",
              ",name,age,education,sex,income<br>0,254343,36,1,1,&gt;50k<br>1,476429,33,7,1,&lt;50k<br>2,363510,33,1,0,&gt;50k<br>")
    )
  }
}