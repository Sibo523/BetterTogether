package com.example.bettertogether

import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath

class RoomsActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomsAdapter: AdapterRooms
    private val roomsList = mutableListOf<DocumentSnapshot>() // Store entire document snapshots

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rooms)

        recyclerView = findViewById(R.id.rooms_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        roomsAdapter = AdapterRooms(roomsList) { document ->
            openRoom(document.id)
        }
        recyclerView.adapter = roomsAdapter

        loadUserRooms()
    }

    private fun loadUserRooms() {
        val user = auth.currentUser
        if(user == null){
            toast("Please log in to see your rooms.")
            navigateToLogin()
        } else{
            db.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        roomsList.clear()
                        val roomIds = document.get("rooms") as? List<Map<String, Any>>
                        if (roomIds != null && roomIds.size > 0) {
                            val ids = roomIds.mapNotNull { it["roomId"] as? String }
                            db.collection("rooms")
                                .whereIn(FieldPath.documentId(), ids)
                                .get()
                                .addOnSuccessListener { querySnapshot ->
                                    roomsList.addAll(querySnapshot.documents)
                                    roomsAdapter.notifyDataSetChanged()
                                }
                                .addOnFailureListener { exception ->
                                    toast("Error fetching rooms: ${exception.message}")
                                }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    toast("Error fetching user data: ${exception.message}")
                }
        }
    }
}
