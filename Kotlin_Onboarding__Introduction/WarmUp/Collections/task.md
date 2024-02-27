In this task we will implement the `countExactMatches` function.

### Task

Implement the _countExactMatches_ function. 
Given the `guess` and the `secret`, the function should output the number 
of letters that match exactly down to position.

You can implement this function in any possible way, but we _recommend_ to look into the `filterIndexed` built-in function.

<div class="Hint" title="Click me to learn more about filterIndexed built-in function">

Kotlin has many built-in functions: e.g., we can filter and manipulate with not only the elements from the list but also list indices.
For example, given two words, we need to build a new word, which consists of the characters that occur in both words at the same positions.
The classic way is:
```kotlin
val secondWord = "AACAAA"
var result = ""
for ((index, symbol) in "ABCDDD".withIndex()) {
   if (secondWord[index] == symbol) {
      result += symbol
   }
}
// The result will be: "AC"
```

But we can also use the [`filterIndexed`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/filter-indexed.html) function to make code shorter:
```kotlin
val secondWord = "AACAAA"
// The result will be: "AC"
"ABCDDD".filterIndexed { index, symbol -> secondWord[index] == symbol }
```

By the way, in this case, the lambda expression takes two arguments, so we use custom names for the arguments (`index` and `symbol`), not `it`.
</div>

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to see examples of the `countExactMatches` function's work">

Here are several examples of the _countExactMatches_ function's work:

- guess = "ACEB", secret = "BCDF", result = 1;
- guess = "ABCD", secret = "DCBA", result = 0;
- guess = "AAAA", secret = "ABBB", result = 1;
- guess = "BBBB", secret = "BBDH", result = 2.
</div>

<div class="Hint" title="Click me to learn the main idea of the algorithm">

The main idea of the algorithm is to keep only those letters that are equal and have the same index. 
Next, we can just return the number of these letters.
To find the number of exact matches, you can consider the <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/filter-indexed.html"><code>filterIndexed</code></a> function.
</div>

<div class="Hint" title="Click me to learn how to get the number elements in the list">

To get the number of characters in the word, you can use <a href="https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/length.html#length"><code>length</code></a>:
`"ABCDDD".length` will return `6`, since the string contains `6` letters.  
</div>
