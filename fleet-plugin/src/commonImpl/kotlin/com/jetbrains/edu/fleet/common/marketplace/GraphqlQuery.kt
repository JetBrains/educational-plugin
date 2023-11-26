package com.jetbrains.edu.fleet.common.marketplace

object GraphqlQuery {
  fun lastUpdateId(courseId: Int) = """
    query {
      updates(
        search: {
          filters: [{ field: "pluginId", value:     ${courseId}     }]
          max: 1
        }
      ) {
        total
        updates {
          id
          version
        }
      }
    }
  """.trimIndent()
}