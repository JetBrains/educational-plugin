### 1. Lambda expressions

Often built-in functions that work with collections accept [lambda expressions](https://kotlinlang.org/docs/lambdas.html#lambda-expressions-and-anonymous-functions).
We will talk about them in detail later, but currently it is enough to know that:
- they offer a special way to tell built-in functions what they should do with _each_
  element from the collection;
- they have a special syntax.

Consider the following example that traverses a word and keeps only the `A` symbols:
```kotlin
var result = ""
for (symbol in "ABCDDD") {
   if (symbol == 'A') {
      result += symbol
   }
}
```

We used the already familiar constructions like `for` and `if`, but it can also be rewritten via built-in functions and lambda expressions:

```kotlin
// Keep only A symbol
"ABCDDD".filter { symbol: Char -> symbol == 'A' }
```
In this case, we are using a lambda expression (a condition),
which will be applied to **each** element of the collection via the built-in function `filter`.
The lambda expression takes one parameter `Char` (character) and compares it with the character `A`
(for characters we need to use single quotes).

The `->` sign indicates the end of the left part of the lambda expression with the arguments.
The right part of the lambda expression (after `->`) says what **exactly** we should do: e.g., compare the `symbol` with 'A'.

### 2. Different ways to work with arguments in lambda expressions

In the previous example, we used the full form for the lambda expression arguments: the name and type of the argument.

If the type of the argument is clear from the context (as it usually is with collections),
then the type can be omitted:
```kotlin
"ABCDDD".filter { symbol -> symbol == 'A' }
```

Also, if you need only one argument,
then this argument already has a built-in name `it`, and in this case it can also be omitted:
```kotlin
"ABCDDD".filter { it == 'A' }
```