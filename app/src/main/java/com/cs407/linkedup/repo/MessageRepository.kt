package com.cs407.linkedup.repo

import com.cs407.linkedup.data.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID


//structure in database for messages
// main database:
// ---|users:
// ------| ... all user info
// ---| conversations
// ------| covnersation by id
// ---------| participants: [userA, userB, ...]
// ---------| ... rest of message data like time staps etc.
// ---------| messages:
// ------------| ... all mesage data
class MessageRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val roomsRef = db.collection("chatRooms")

    // send a message to a room
    suspend fun sendMessage(
        roomId: String,
        senderId: String,
        senderName: String,
        text: String
    ) {
        val msg = hashMapOf(
            "roomId" to roomId,
            "senderId" to senderId,
            "senderName" to senderName,
            "text" to text,
            "time" to FieldValue.serverTimestamp()
        )

        roomsRef
            .document(roomId)
            .collection("messages")
            .add(msg)
            .await()
    }

    fun listenForMessages(
        roomId: String,
        onMessages: (List<ChatMessage>) -> Unit
    ): ListenerRegistration {
        return roomsRef
            .document(roomId)
            .collection("messages")
            .orderBy("time", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val messages = snapshot.documents.map { doc ->
                    val data = doc.data ?: emptyMap<String, Any>()

                    val senderId = data["senderId"] as? String ?: ""
                    val senderName = data["senderName"] as? String ?: ""
                    val text = data["text"] as? String ?: ""

                    val ts = data["time"] as? Timestamp ?: Timestamp.now()

                    ChatMessage(
                        id = doc.id,
                        roomId = roomId,
                        senderId = senderId,
                        senderName = senderName,
                        text = text,
                        time = ts
                    )
                }

                onMessages(messages)
            }
    }
}