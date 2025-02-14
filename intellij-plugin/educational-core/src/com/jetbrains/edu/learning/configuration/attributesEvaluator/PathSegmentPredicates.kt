package com.jetbrains.edu.learning.configuration.attributesEvaluator

/**
 * Predicate on a PathSegment's name
 */
interface PathSegmentNamePredicate {
  fun accepts(name: String): Boolean
}

/**
 * Matches any name of a PathSegment
 */
internal val predicateAny = object : PathSegmentNamePredicate {
  override fun accepts(name: String): Boolean = true
  override fun toString(): String = "*"
}

/**
 * Matches PathSegment names with the specified [extension]
 */
internal class ExtensionPredicate(private val extension: String) : PathSegmentNamePredicate {
  override fun accepts(name: String): Boolean = name.endsWith(".$extension")
  override fun toString(): String = "ext=$extension"
}

/**
 * Predicate on the entire path segment, including its name and whether it is a directory or a file.
 */
internal interface PathSegmentPredicate {
  fun accepts(segment: PathSegment): Boolean

  companion object {

    /**
     * Creates a predicate on a [PathSegment] by specifying separately a list of [PathSegmentNamePredicate]s (at least one of them
     * should match), and specifying whether it should be a file or a directory.
     *
     * [args] lists [PathSegmentNamePredicate]s, but they might be represented in different ways. Each element of [args] could be either:
     * - [PathSegmentNamePredicate], in this case it is taken as is
     * - [String], in this case the name must fully match
     * - [Regex], in this case there should be a match inside the segment's name (not necessarily a full match).
     *
     * For example, if `args = listOf("a.txt", ".doc$"), then all files with the `doc` extension, and the file `a.txt` are matched.
     */
    fun create(args: Iterable<Any>, mustBeDirectory: Boolean, mustBeFile: Boolean): PathSegmentPredicate = object : PathSegmentPredicate {
      override fun accepts(segment: PathSegment): Boolean {
        if (!segment.check(mustBeDirectory, mustBeFile)) return false

        return args.any { accepts(it, segment) }
      }

      override fun toString(): String = args.joinToString(prefix = "(", postfix = ")", separator = " | ") {
        when (it) {
          is String -> it
          is Regex -> "/$it/"
          is PathSegmentNamePredicate -> it.toString()
          else -> "??"
        }
      }
    }

    private fun accepts(arg: Any, segment: PathSegment): Boolean {
      return when (arg) {
        is String -> segment.name == arg
        is Regex -> arg.containsMatchIn(segment.name)
        is PathSegmentNamePredicate -> arg.accepts(segment.name)
        else -> error("Rule arguments must be either String, Regex, or PathSegmentNamePredicate. But it is $arg (of class ${arg.javaClass})")
      }
    }
  }
}