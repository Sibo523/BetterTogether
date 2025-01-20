package com.example.bettertogether

import android.os.Bundle
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot

class ExplorerActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomsAdapter: AdapterRooms
    private val roomsList = mutableListOf<DocumentSnapshot>() // Store entire document snapshots
    private val filteredRoomsList = mutableListOf<DocumentSnapshot>() // Filtered list for search

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explorer)

        recyclerView = findViewById(R.id.rooms_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        roomsAdapter = AdapterRooms(filteredRoomsList) { document ->
            openRoom(document.id)
        }
        recyclerView.adapter = roomsAdapter

        loadAllRooms()
        setupSearchBar()
    }

    private fun loadAllRooms() {
        db.collection("rooms")
            .get()
            .addOnSuccessListener { querySnapshot ->
                roomsList.clear()
                roomsList.addAll(querySnapshot.documents)
                filteredRoomsList.clear()
                filteredRoomsList.addAll(roomsList) // Initially display all rooms
                roomsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                toast("Error fetching rooms: ${exception.message}")
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
                roomsList.filter { document ->
                    val roomName = document.getString("name") ?: "Unnamed Room"
                    roomName.contains(searchQuery, ignoreCase = true)
                }
            )
        }
        roomsAdapter.notifyDataSetChanged() // Refresh the adapter
    }

}
