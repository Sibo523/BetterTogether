package com.example.bettertogether

import android.os.Bundle
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ExplorerActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomsAdapter: RoomsAdapter
    private val roomsList = mutableListOf<Triple<String, String, Boolean>>() // Triple<roomID, roomName, isPublic>
    private val filteredRoomsList = mutableListOf<Triple<String, String, Boolean>>() // Filtered list for search

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explorer)

        recyclerView = findViewById(R.id.rooms_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        roomsAdapter = RoomsAdapter(filteredRoomsList) { roomId ->
            openRoom(roomId)
        }
        recyclerView.adapter = roomsAdapter

        loadAllRooms()
        setupSearchBar()
        setupBottomNavigation()
    }

    private fun loadAllRooms() {
        db.collection("rooms")
            .get()
            .addOnSuccessListener { documents ->
                roomsList.clear()
                filteredRoomsList.clear()
                for (document in documents) {
                    val roomId = document.id
                    val roomName = document.getString("name") ?: "Unnamed Room"
                    val isPublic = document.getBoolean("isPublic") ?: false // Default to false if not provided
                    roomsList.add(Triple(roomId, roomName, isPublic))
                }
                filteredRoomsList.addAll(roomsList) // Initially, show all rooms
                roomsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching rooms: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSearchBar() {
        val searchView = findViewById<SearchView>(R.id.search_view)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterRooms(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterRooms(newText)
                return true
            }
        })
    }

    private fun filterRooms(query: String?) {
        val searchQuery = query?.trim() ?: ""
        filteredRoomsList.clear()

        if (searchQuery.isEmpty()) {
            filteredRoomsList.addAll(roomsList) // Show all rooms if search is empty
        } else {
            filteredRoomsList.addAll(
                roomsList.filter { it.second.contains(searchQuery, ignoreCase = true) }
            )
        }
        roomsAdapter.notifyDataSetChanged() // Refresh the adapter
    }
}
