You will be given:
1. A **task description** describing a programming problem.
2. An **initial code snippet** in ${programmingLanguage} that was given to a student.
3. A **patch** representing the modifications made by the student.

Your task is to:
- Analyze the student's changes and determine if they follow good coding practices and style.
- If improvements are necessary, refactor only the modified lines while maintaining the intended logic.
- Preserve any comment insertions or deletions exactly as they appear in the patch.
- Add comments **only to the modified lines** to explain improvements, but **do not add comments to unchanged lines**.
- If no changes are needed, return the original patch as-is.
- Output the final **patch** with necessary refinements.

#### **Example 1: When a Change is Needed**
##### **Input**
**Task description:**
```
## String multiplication
Python supports string-by-number multiplication (but not the other way around!).
Use `not_yet_food` to generate `food`.
```
**Initial Code:**
```python
not_yet_food = "cous"
food = 'TODO'
```
**Patch Applied:**
```diff
---
+++
@@ -1,2 +1,3 @@
-food = 'TODO'
+food = "cous"*2
+print(food)
```

##### **Expected Output**
```diff
---
+++
@@ -1,3 +1,3 @@
-food = 'TODO'
+food = not_yet_food * 2 # Improved: Use 'not_yet_food' instead of hardcoding "cous".
```

---

#### **Example 2: When No Changes Are Needed**
##### **Input**
**Initial Code:**
```python
not_yet_food = "cous"
food = 'TODO'
```
**Patch Applied:**
```diff
---
+++
@@ -1,2 +1,3 @@
-food = 'TODO'
+food = not_yet_food * 2
+print(food)
```

##### **Expected Output**
```diff
---
+++
@@ -1,2 +1,3 @@
-food = 'TODO'
+food = not_yet_food * 2
+print(food)
```

---
