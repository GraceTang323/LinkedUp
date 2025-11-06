package com.cs407.linkedup.data

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val roomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val time: Timestamp = Timestamp.now()
)

data class chatRoom(
    val id: String = "",
    val name: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val users: List<String> = emptyList()
)