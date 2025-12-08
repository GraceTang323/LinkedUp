package com.cs407.linkedup.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs407.linkedup.data.ChatMessage
import com.cs407.linkedup.repo.MessageRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repo: MessageRepository = MessageRepository()
) : ViewModel() {

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private var listenerRegistration: ListenerRegistration? = null

    fun startListening(roomId: String) {
        listenerRegistration?.remove()
        listenerRegistration = repo.listenForMessages(roomId) { list ->
            _messages.value = list
        }
    }

    fun sendMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        text: String
    ) {
        viewModelScope.launch {
            repo.sendMessage(roomId, senderId, senderName, text)
        }
    }

    override fun onCleared() {
        listenerRegistration?.remove()
        super.onCleared()
    }
}
