package com.example.bettertogether

import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath

class ExplorerActivity : BaseActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomsAdapter: AdapterRooms
    private val roomsList = mutableListOf<DocumentSnapshot>() // Store entire document snapshots
    private val filteredRoomsList = mutableListOf<DocumentSnapshot>() // Filtered list for search

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explorer)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val adapter = AdapterRoomsPager(this)
        viewPager.adapter = adapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val currentView = adapter.getPageView(position)
                if (currentView != null) {
                    when (position) {
                        0 -> initExplorerView(currentView)
                        1 -> initRoomsView(currentView)
                    }
                }
            }
        })
    }

    private fun initExplorerView(view: View){
        recyclerView = view.findViewById(R.id.explorer_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        roomsAdapter = AdapterRooms(filteredRoomsList) { document ->
            openRoom(document.id)
        }
        recyclerView.adapter = roomsAdapter

        loadAllRooms()
        val searchView = view.findViewById<SearchView>(R.id.search_view)
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

    private fun initRoomsView(view: View){
        recyclerView = view.findViewById(R.id.rooms_recycler_view)
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
