package com.initlauncher.lockscreen

/**
 * BigMoney-ne style figlet font renderer.
 * Uses '@' as a placeholder for '$' in raw string literals (to avoid Kotlin template interpolation).
 * Call render() to get a multi-line ASCII art string for the given time/text.
 */
object AsciiBigFont {

    // Each entry is 8 rows. '@' will be replaced with '$' at render time.
    private val CHARS: Map<Char, Array<String>> = mapOf(
        '0' to arrayOf(
            """ /@@@@@@  """,
            """| @@__  @@""",
            """| @@  \ @@""",
            """| @@  | @@""",
            """| @@  | @@""",
            """| @@  \ @@""",
            """|  @@@@@@/""",
            """ \______/ """
        ),
        '1' to arrayOf(
            """   /@@   """,
            """  /@@@@  """,
            """ |_  @@  """,
            """   | @@  """,
            """   | @@  """,
            """   | @@  """,
            """  /@@@@@@""",
            """ |______/"""
        ),
        '2' to arrayOf(
            """ /@@@@@@  """,
            """| @@__  @@""",
            """|__/  \ @@""",
            """    /@@@@/""",
            """   /@@__  """,
            """  | @@    """,
            """  | @@@@@@""",
            """  |______/"""
        ),
        '3' to arrayOf(
            """ /@@@@@@  """,
            """| @@__  @@""",
            """|__/  \ @@""",
            """   /@@@@@@""",
            """  /@@__  @@""",
            """ | @@  \ @@""",
            """ |  @@@@@@/""",
            """  \______/ """
        ),
        '4' to arrayOf(
            """  /@@  /@@""",
            """ | @@ | @@""",
            """ | @@ | @@""",
            """ |  @@@@@/""",
            """  \____  @""",
            """       | @@""",
            """       | @@""",
            """       |__/"""
        ),
        '5' to arrayOf(
            """  /@@@@@@@@""",
            """ | @@_____/""",
            """ | @@      """,
            """ | @@@@@   """,
            """ |_____  @@""",
            """  /@@  \ @@""",
            """ |  @@@@@@/""",
            """  \______/ """
        ),
        '6' to arrayOf(
            """   /@@@@@ """,
            """  /@@__  @@""",
            """ | @@  \__/""",
            """ | @@@@@   """,
            """ | @@__  @@""",
            """ | @@  \ @@""",
            """ |  @@@@@@/""",
            """  \______/ """
        ),
        '7' to arrayOf(
            """ /@@@@@@@@""",
            """ \___  @@ """,
            """      @@  """,
            """     @@   """,
            """    @@    """,
            """   @@     """,
            """  @@      """,
            """ |__/     """
        ),
        '8' to arrayOf(
            """ /@@@@@@  """,
            """| @@__  @@""",
            """| @@  \ @@""",
            """|  @@@@@@/""",
            """ /@@__  @@""",
            """| @@  \ @@""",
            """|  @@@@@@/""",
            """ \______/ """
        ),
        '9' to arrayOf(
            """  /@@@@@@  """,
            """ | @@__  @@""",
            """ | @@  \ @@""",
            """ |  @@@@@@/""",
            """  \_____  @@""",
            """   /@@  \ @@""",
            """  |  @@@@@@/""",
            """   \______/ """
        ),
        ':' to arrayOf(
            """     """,
            """  @@ """,
            """ |__/""",
            """     """,
            """  @@ """,
            """ |__/""",
            """     """,
            """     """
        ),
        ' ' to arrayOf(
            """   """,
            """   """,
            """   """,
            """   """,
            """   """,
            """   """,
            """   """,
            """   """
        )
    )

    fun render(text: String): String {
        val rows = Array(8) { StringBuilder() }
        for (ch in text) {
            val charRows = CHARS[ch] ?: CHARS[' ']!!
            val width = charRows.maxOf { it.length }
            for (i in 0..7) {
                rows[i].append(charRows[i].padEnd(width))
            }
        }
        return rows.joinToString("\n") { it.toString().replace('@', '$').trimEnd() }
    }
}
