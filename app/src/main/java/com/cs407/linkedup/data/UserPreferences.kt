package com.cs407.linkedup.data

data class UserPreferences(
    val interests: List<String> = emptyList(),
    val classes: List<String> = emptyList(),
)