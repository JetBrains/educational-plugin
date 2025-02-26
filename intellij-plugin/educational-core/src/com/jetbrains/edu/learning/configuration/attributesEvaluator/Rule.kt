package com.jetbrains.edu.learning.configuration.attributesEvaluator

import com.jetbrains.edu.learning.courseFormat.Course

internal typealias AttributesMutator = (CourseFileAttributesMutable) -> Unit

/**
 * A PathSegmentPredicate together with the information, whether this path segment should go directly after the previous one.
 * See the [Rule] documentation.
 */
internal data class PathSegmentSpecification(
  val direct: Boolean,
  val accepts: PathSegmentPredicate
) {
  override fun toString(): String = buildString {
    append(if (direct) ">" else "")
    append(accepts)
  }
}

/** A CSS-style rule on a sequence of path segments.
* It consists of several [PathSegmentSpecification]s, and matches paths that have a sub-list (not necessarily consequent) of path
* segments matching these specifications.
*
* For example, a Rule
* `dir("dir1")  dir("dir2")  file("a.txt")` matches paths that correspond to files `a.txt` located in a directory `dir2`, located in
* directory `dir1`: All these match: `dir1/dir2/a.txt`, `a/dir1/b/dir2/c/a.txt`.
*
* `dir("dir1", direct=true) dir("dir2", direct=true) file(a.txt)` corresponds to paths: `dir1/dir2/a.txt`, `dir1/dir2/x/y/z/a.txt`.
*
* `dir("a", "b", "c") file("a.txt")` corresponds to paths of the form: `xxx/a/yyy/a.txt`, `xxx/b/yyy/a.txt`, `xxx/c/yyy/a.txt`.
* */
internal data class Rule(
  /**
   * The more specific the rule is, the later it is applied. So attributes of a file are taken from the most specific rules.
   */
  val specificity: Int,
  val setupAttributes: MutableList<AttributesMutator> = mutableListOf(),
  private val pathSpecifications: List<PathSegmentSpecification>,
  private val coursePredicate: CoursePredicate? = null
) {

  /**
   * Is implemented with a dynamic-algorithm:
   *  - `fullyMatches[i,j]` means that `pathSpecifications[0..i]` matches `pathSegments[0..j]`.
   *  - `prefixMatches[i,j]` means that `pathSpecifications[0..i]` matches some **prefix** of `pathSegments[0..j]`.
   *  In other words, `pathSpecifications[0..i]` matches `pathSegments[0..k]` for `k <= j`.
   */
  fun matches(pathSegments: List<PathSegment>): Boolean {
    val segmentsCount = pathSegments.size
    val specificationsCount = pathSpecifications.size

    val prefixMatches: Array<BooleanArray> = Array(specificationsCount + 1) { BooleanArray(segmentsCount + 1) }
    val fullyMatches: Array<BooleanArray> = Array(specificationsCount + 1) { BooleanArray(segmentsCount + 1) }

    prefixMatches[0] = BooleanArray(segmentsCount + 1) { true }
    fullyMatches[0][0] = true

    for (specificationIndex in 1..specificationsCount) {
      val specification = pathSpecifications[specificationIndex - 1]

      for (segmentIndex in 1..segmentsCount) {
        val segment = pathSegments[segmentIndex - 1]

        fullyMatches[specificationIndex][segmentIndex] = when {
          !specification.accepts.accepts(segment) -> false
          specification.direct -> fullyMatches[specificationIndex - 1][segmentIndex - 1]
          else -> prefixMatches[specificationIndex - 1][segmentIndex - 1]
        }

        prefixMatches[specificationIndex][segmentIndex] =
          fullyMatches[specificationIndex][segmentIndex] || prefixMatches[specificationIndex][segmentIndex - 1]
      }
    }

    return fullyMatches[specificationsCount][segmentsCount]
  }

  fun matches(course: Course?): Boolean {
    if (coursePredicate == null) return true
    if (course == null) return false
    return coursePredicate(course)
  }

  override fun toString(): String {
    return "Rule{$specificity}${pathSpecifications.joinToString("")}"
  }
}
