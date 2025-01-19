package com.example.bettertogether

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class PageChat : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: Button

    private val chatMessages = mutableListOf<Message>()
    private lateinit var chatAdapter: AdapterChat

    private var roomId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.page_chat) // וודא שיצרת layout בשם page_chat

        initViews()

        roomId = intent.getStringExtra("roomId")
        if (roomId != null) {
            setupChat(roomId!!)
        } else {
            toast("Room ID is missing")
            finish()
        }

        setupBottomNavigation()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.chatRecyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)

        // אתחול ה-Adapter עבור RecyclerView
        chatAdapter = AdapterChat(chatMessages)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = chatAdapter
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupChat(roomId: String) {
        db.collection("rooms").document(roomId).collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    toast("Error loading messages.")
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    chatMessages.clear()
                    for (doc in snapshots) {
                        chatMessages.add(doc.toObject(Message::class.java))
                    }
                    chatAdapter.notifyDataSetChanged()
                    if (chatMessages.isNotEmpty()) {
                        recyclerView.scrollToPosition(chatMessages.size - 1)
                    }
                }
            }

        // שליחת הודעה
        sendButton.setOnClickListener {
            val messageText = messageInput.text.toString()
            if (messageText.isNotBlank()) {
                sendMessage(roomId, messageText)
            } else {
                toast("Message cannot be empty.")
            }
        }
    }

    private fun sendMessage(roomId: String, messageText: String) {
        val currentUser = auth.currentUser
        val senderName = currentUser?.displayName ?: currentUser?.email ?: "Anonymous"

        val message = Message(sender = senderName, message = messageText)
        db.collection("rooms").document(roomId).collection("messages")
            .add(message)
            .addOnSuccessListener {
                messageInput.text.clear()
            }
            .addOnFailureListener {
                toast("Error sending message.")
            }
    }
}
