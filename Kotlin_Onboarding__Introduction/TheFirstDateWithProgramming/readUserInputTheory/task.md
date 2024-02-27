We have previously talked about the _built-in_ functions in Kotlin.
In addition to the already familiar functions for output to the console,
Kotlin also has the [`readlnOrNull`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.io/readln-or-null.html) function, which reads values entered by the user and stores them in a variable:

```kotlin
val myValue = readlnOrNull()
// The myValue variable stores the user's input
```

For example, in our **Story twister** project,
we need to ask the user a question and then remember their answer.
That is exactly what the `readlnOrNull` function is for.
The function tells the program to stop and wait for
the user to enter some value into the console; then it allows the program to receive the value and, for example, write it into a variable.

It is important that in this case you **cannot** write:
```kotlin
val myValue: String = readlnOrNull()
// The myValue variable stores the user's input
```

The correct option is:
```kotlin
val myValue: String? = readlnOrNull()
// The myValue variable stores the user's input
```

This relates to null safety in Kotlin, which we will consider later. If you are interested in this topic, you can read more in the [documentation](https://kotlinlang.org/docs/null-safety.html).
