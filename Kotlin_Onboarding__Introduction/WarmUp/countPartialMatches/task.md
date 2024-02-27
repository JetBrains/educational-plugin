In this task we will implement the `countPartialMatches` function.

### Task

Implement the _countPartialMatches_ function.
The function takes the _secret_ and the _guess_,
and returns the number of matched letters between them that are not in the same positions.

You can implement this function in any possible way, 
but we _recommend_ reuse already implemented in the previous tasks `countAllMatches` and `countExactMatches` functions.

If you have any difficulties, **hints will help you solve this task**.

----

### Hints

<div class="Hint" title="Click me to see examples of the `countPartialMatches` function's work">

Here are several examples of the _countPartialMatches_ function's work:

- guess = "ACEB", secret = "BCDF", result = 1;
- guess = "ABCD", secret = "DCBA", result = 4;
- guess = "AAAA", secret = "ABBB", result = 0;
- guess = "BBBB", secret = "BBDH", result = 0.
</div>

<div class="Hint" title="Click me to learn the main idea of the algorithm">

Since we already have functions that calculate count of all matches and count of exact matches, 
we can just subtract count of exact matches from count of all matches to get the right value.
</div>
