### 1. `String` as a `List` of characters

What does a string usually consist of?
Actually, a string is a sequence of characters (letters in our case).
Therefore, when working with strings, we can think of them as a _list of letters_.

Kotlin has a special data type for working with such sequences - [`List`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-list/).
For now, we need to know the following:
1) `List` is a collection that contains data of the _same_ type, e.g., only strings or ints.
3) `List` is an ordered collection (each element has a position).
   The position of the first element in a list is zero.

Let's look at an example in the context of our task:
the word `ABCDDD` can be divided into six letters: `A`, `B`, `C`, `D`, `D`, `D`.
The list in this case will consist of six elements - `A`, `B`, `C`, `D`, `D`, `D`,
and each of them has its position: `A` - 0, `B` - 1, `C` - 2, `D` - 3, `D` - 4, `D` - 5.

### 2. How to get an element from the list

To get an element in a list by the position number,
it is enough to refer to the number in square brackets:
```kotlin
// Get B character
"ABCDDD"[1]
```

**Note**: the index of the first element in the list is zero.
