package com.cs407.linkedup.data

data object Preferences {
    val categoryToInterests: Map<String, List<String>> = mapOf(
        "STEM" to listOf("Java", "Data Programming", "Graphics", "HTML and CSS", "Prototyping", "Kotlin", "AI", "Robotics"),
        "HUMANITIES" to listOf("Philosophy", "History", "Anthropology", "Asian Studies", "Communications", "Psychology"),
        "LEGAL STUDIES" to listOf("Legal Writing", "Criminal Law", "Contracts")
    )

    val classes: Map<String, List<String>> = mapOf(
        "CS" to listOf("CS 300", "CS 400", "CS 407", "CS 571"),
        "ECE" to listOf("ECE 252", "ECE 354", "ECE 352", "ECE 203"),
        "MATH" to listOf("MATH 431", "MATH 222", "MATH 221")
    )
}