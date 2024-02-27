During this lesson we will learn theory about loops in Kotlin.

#### 1. What the loops are

Sometimes, the same actions need to be repeated several times:
for example, to play several rounds of a game or print the same text on the screen.
To solve this problem, you can use loops.
Loops can be executed while a certain condition is true (the [`while`](https://kotlinlang.org/docs/basic-syntax.html#while-loop) loop)
or can be repeated a certain number of times (the [`for`](https://kotlinlang.org/docs/control-flow.html#for-loops) loop).

#### 2. The `while` and `do-while` loops

The `while` loops often use `Boolean` values, for example:
```kotlin
while(y < 10) {
    // To do something
}
```
It will execute the actions (the loop's body) while the value in `y` is less than ten.
Here, the condition will be checked _first_, and next, if it is true,
the loop's body will be executed.
If you need another scenario: _first_ execute the loop's body and then check the condition,
you should use the `do-while` loop. In such a case, the body will be executed at least one time:
```kotlin
do {
    // To do something
} while(y < 10)
```

#### 3. The `for` loop

The `for` loops often use [`Ranges`](https://kotlinlang.org/docs/basic-syntax.html#ranges) to define
how many times the body of the loop will be executed:
```kotlin
for (i in 1..3) {
  // To do something
}
```
In this case, the body of the loop will be executed three times: `1 <= i <= 3`.
Kotlin has [several ways](https://kotlinlang.org/docs/idioms.html#iterate-over-a-range)
to define the borders of a range; for example, the range `1 <= i < 3` can be defined as follows:
```kotlin
for (i in 1 until 3) {
  // To do something
}
```

#### 4. `Boolean` variables in the loop conditions

If you need to work with `Boolean` variables, you should omit the comparing part in the condition:
```kotlin
while (b == true) { 
    // ... 
}
```
vs
```kotlin
while (b) {  
    // ... 
}
```

An opposite example:
```kotlin
while (b == false) {
    // ... 
}
```
vs
```kotlin
while (!b) {
    // ... 
}
```