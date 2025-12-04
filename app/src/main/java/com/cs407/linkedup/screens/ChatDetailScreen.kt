package com.cs407.linkedup.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 数据类 - 你的组员会用实际数据替换
data class ChatMessage(
    val messageId: String = "",
    val senderId: String,
    val receiverId: String,
    val message: String,
    val timestamp: Long,
    val isLiked: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    userName: String,
    currentUserId: String = "currentUser",
    onBackClick: () -> Unit = {}
) {
    var messageText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Mock messages - 你的组员会替换成真实数据
    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                messageId = "1",
                senderId = "otherUser",
                receiverId = currentUserId,
                message = "I am at Union South.",
                timestamp = System.currentTimeMillis() - 10000,
                isLiked = false
            ),
            ChatMessage(
                messageId = "2",
                senderId = currentUserId,
                receiverId = "otherUser",
                message = "Me too!",
                timestamp = System.currentTimeMillis() - 8000,
                isLiked = true
            ),
            ChatMessage(
                messageId = "3",
                senderId = "otherUser",
                receiverId = currentUserId,
                message = "Wanna meet up?",
                timestamp = System.currentTimeMillis() - 5000,
                isLiked = false
            ),
            ChatMessage(
                messageId = "4",
                senderId = currentUserId,
                receiverId = "otherUser",
                message = "Yes!",
                timestamp = System.currentTimeMillis() - 2000,
                isLiked = false
            )
        )
    }

    // Auto scroll to bottom when new message is added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = userName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFD5D8DC)
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                message = messageText,
                onMessageChange = { messageText = it },
                onSendClick = {
                    if (messageText.isNotBlank()) {
                        // 你的组员会替换这里的逻辑
                        messages.add(
                            ChatMessage(
                                messageId = System.currentTimeMillis().toString(),
                                senderId = currentUserId,
                                receiverId = "otherUser",
                                message = messageText,
                                timestamp = System.currentTimeMillis(),
                                isLiked = false
                            )
                        )
                        messageText = ""
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFD5D8DC))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages) { message ->
                    MessageBubble(
                        message = message,
                        isCurrentUser = message.senderId == currentUserId,
                        onLikeClick = {
                            // 你的组员会替换这里的逻辑
                            val index = messages.indexOf(message)
                            if (index != -1) {
                                messages[index] = message.copy(isLiked = !message.isLiked)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isCurrentUser: Boolean,
    onLikeClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (isCurrentUser) {
            Spacer(modifier = Modifier.width(50.dp))
        }

        Card(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isCurrentUser) 20.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 20.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (isCurrentUser) Color(0xFFB8E6C3) else Color.White
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Box {
                Text(
                    text = message.message,
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(12.dp)
                )

                // Heart icon for received messages
                if (!isCurrentUser && message.isLiked) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Liked",
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier
                            .size(20.dp)
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    )
                }
            }
        }

        if (!isCurrentUser) {
            Spacer(modifier = Modifier.width(50.dp))
        }
    }
}

@Composable
fun ChatInputBar(
    message: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit
) {
    Surface(
        color = Color(0xFFB8E6C3),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Input field
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = message,
                        onValueChange = onMessageChange,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color.Black
                        ),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (message.isEmpty()) {
                                Text(
                                    text = "Start chatting...",
                                    fontSize = 16.sp,
                                    color = Color.Gray
                                )
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Send button
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White,
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(onClick = onSendClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}