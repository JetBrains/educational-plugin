In the upcoming steps, we will implement some helper functions to create the application.

### Task

Implement the `fillPatternRow` function, which accepts a `patternRow` (one line from the pattern) and `patternWidth`
and adds the row `separator` to extend the line to the `patternWidth` size.

<div class="hint" title="Click me to see the new signature of the getPatternHeight function">

The signature of the function is:
```kotlin
fun fillPatternRow(patternRow: String, patternWidth: Int): String
```
</div>

Please throw an `IllegalStateException` if `patternRow.length > patternWidth`.

For the separator, please use the predefined variable `separator`, which stores a space.
```kotlin
println("This is the value from the separator variable: $separator.") // This is the value from the separator variable:  .
```

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="hint" title="Click me to see details about the `repeat` built-in function">

To generate a string that consists of some repeated symbols, you can use the <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/repeat.html"><code>repeat</code></a> function, e.g.:
  ```kotlin
  println("a".repeat(5)) // aaaaa
  ```
</div>

<div class="hint" title="Click me to learn the main idea of the `fillPatternRow` function">

To implement the `fillPatternRow` function, you just need to check 
if the length of the current pattern row is less than the pattern width. 
If it is `true`, add the necessary number of spaces at the end of the row.
</div>
