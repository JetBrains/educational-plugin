In this step, we will add a function to generate the initial hidden secret using underscores.

### Task

Implement the `getHiddenSecret` function, which accepts `wordLength`
and generates the initial hidden secret using underscores: e.g., for `wordLength` `4`, the result will be `_ _ _ _`.

<div class="hint" title="Click me to see the new signature of the getHiddenSecret function">

The signature of the function is:
```kotlin
fun getHiddenSecret(wordLength: Int): String
```
</div>

You can also use the already defined variables `separator` and `underscore`, which store a space and an underscore respectively:
```kotlin
println("This is the value from the separator variable: $separator.") // This is the value from the separator variable:  .
println("This is the value from the underscore variable: $underscore.") // This is the value from the underscore variable: _.
```

You can implement this function in any way you choose, but we _recommend_ looking into the [`joinToString`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/join-to-string.html) function.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to learn more about the joinToString function">

To join a list of elements into a string,
you can use the [`joinToString`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/join-to-string.html)
function, passing the _separator_ — a character (or _string_)
that will be used to separate the elements — as an argument:
```kotlin
// The separator is "; ", the resulting string is: "6_6_6_6_6"
List(5) { 6 }.joinToString("_")
// The default separator is ", ", the resulting string is: "A, A, A, A, A"
List(5) { 'A' }.joinToString()
```
</div>
