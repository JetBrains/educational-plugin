1. Here's what Marketplace expects from our `course.zip/course.json`:

    ```json
    {
      "properties": {
        "environment": {
          "description": "Additional information about course environment. For example, Kotlin courses are supported for Android environment",
          "type": [
            "string",
            "null"
          ]
        },
        "summary": {
          "description": "Summary describing what the course is about",
          "type": "string"
        },
        "title": {
          "description": "Title of the course",
          "type": "string"
        },
        "programming_language": {
          "description": "(Deprecated) Old programming language of the course",
          "type": "string"
        },
        "programming_language_id": {
          "description": "Programming language ID of the course",
          "type": "string"
        },
        "programming_language_version": {
          "description": "Programming language version of the course",
          "type": "string"
        },
        "language": {
          "description": "Language code used for course materials",
          "type": "string",
          "examples": [
            "en",
            "ru"
          ]
        },
        "edu_plugin_version": {
          "description": "JetBrains Academy plugin version. Used to calculate compatibility on the marketplace.",
          "type": "string",
          "examples": [
            "3.7-2019.3-5266"
          ]
        },
        "vendor": {
          "description": "Vendor name, email and url information",
          "type": [
            "object",
            "null"
          ],
          "properties": {
            "name": {
              "type": "string",
              "description": "Vendor name"
            },
            "url": {
              "type": "string",
              "description": "Vendor url"
            },
            "email": {
              "type": "string",
              "description": "Vendor email"
            }
          },
          "required": [
            "name"
          ],
          "additionalProperties": false
        },
        "is_private": {
          "description": "Determines if course is private and should not be displayed in browse courses dialog",
          "type": ["boolean", "null"]
        },
        "course_type": {
          "description": "Course type",
          "type": "string",
          "enum": [
            "edu",
            "coursera"
          ]
        },
        "items": {
          "description": "Lessons or sections included in the course",
          "type": [
            "array"
          ],
          "uniqueItems": true
        }
      },
      "required": [
        "title",
        "summary",
        "items",
        "vendor",
        "language",
        "programming_language_id",
        "edu_plugin_version"
      ]
    }
    ```

2. Marketplace course archive could contain `courseIcon.svg` icon in the archive root. This image will be shown on the plugin page.