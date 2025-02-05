package com.example.bettertogether

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath

class ExplorerActivity : BaseActivity() {

    // -------------------------------
    // "All Rooms" data for page 0
    // -------------------------------
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomsAdapter: AdapterRooms
    private val roomsList = mutableListOf<DocumentSnapshot>()
    private val filteredRoomsList = mutableListOf<DocumentSnapshot>()

    // -------------------------------
    // "My Rooms" data for page 1
    // -------------------------------
    private lateinit var yourRoomsAdapter: AdapterEvents
    // (Re-uses roomsList for "my rooms")

    // -------------------------------
    // "Users" data for page 2
    // -------------------------------
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var usersAdapter: AdapterUsers
    private val usersList = mutableListOf<DocumentSnapshot>()
    private val filteredUsersList = mutableListOf<DocumentSnapshot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_explorer)

        val viewPager: ViewPager2 = findViewById(R.id.viewPager)
        val pagerAdapter = AdapterRoomsPager(this)
        viewPager.adapter = pagerAdapter

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val currentView = pagerAdapter.getPageView(position)
                if (currentView != null) {
                    when (position) {
                        0 -> initExplorerView(currentView) // All Rooms
                        1 -> initMyRoomsView(currentView)   // Your Rooms
                        2 -> initUsersView(currentView)     // Users
                    }
                }
            }
        })
    }

    //--------------------------------------------------------------------------
    // Page 0: Explorer (All Rooms)
    //--------------------------------------------------------------------------
    private fun initExplorerView(view: View) {
        recyclerView = view.findViewById(R.id.explorer_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        roomsAdapter = AdapterRooms(filteredRoomsList) { document ->
            openRoom(document.id)
        }
        recyclerView.adapter = roomsAdapter

        // Load all rooms from Firestore
        loadAllRooms()

        // Set up search bar to filter rooms
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
                filteredRoomsList.clear()

                if (querySnapshot.isEmpty) {
                    toast("No rooms found")
                    roomsAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                roomsList.addAll(querySnapshot.documents)
                // By default, show everything in the filtered list
                filteredRoomsList.addAll(roomsList)
                roomsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                toast("Error fetching rooms: ${exception.message}")
            }
    }

    private fun filterRooms(query: String?) {
        val q = query?.trim() ?: ""
        filteredRoomsList.clear()

        if (q.isEmpty()) {
            filteredRoomsList.addAll(roomsList)
        } else {
            val matched = roomsList.filter { doc ->
                val roomName = doc.getString("name") ?: "Unnamed Room"
                roomName.contains(q, ignoreCase = true)
            }
            filteredRoomsList.addAll(matched)
        }
        roomsAdapter.notifyDataSetChanged()
    }

    //--------------------------------------------------------------------------
    // Page 1: "My Rooms"
    //--------------------------------------------------------------------------
    private fun initMyRoomsView(view: View) {
        recyclerView = view.findViewById(R.id.rooms_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        yourRoomsAdapter = AdapterEvents(roomsList) { document ->
            openRoom(document.id)
        }
        recyclerView.adapter = yourRoomsAdapter

        loadUserRooms()
    }

    private fun loadUserRooms() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            toast("Please log in to see your rooms.")
            navigateToLogin()
            return
        }

        db.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    roomsList.clear()
                    val roomIds = getUserActiveRooms(document) // your custom function

                    if (roomIds.isNotEmpty()) {
                        val ids = roomIds.mapNotNull { it["roomId"] as? String }
                        db.collection("rooms")
                            .whereIn(FieldPath.documentId(), ids)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                roomsList.addAll(querySnapshot.documents)
                                yourRoomsAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener { ex ->
                                toast("Error fetching rooms: ${ex.message}")
                            }
                    } else {
                        // If user has no rooms
                        yourRoomsAdapter.notifyDataSetChanged()
                    }
                } else {
                    toast("No user data found.")
                }
            }
            .addOnFailureListener { ex ->
                toast("Error fetching user data: ${ex.message}")
            }
    }

    //--------------------------------------------------------------------------
    // Page 2: "Users"
    //--------------------------------------------------------------------------
    private fun initUsersView(view: View) {
        val searchView = view.findViewById<SearchView>(R.id.search_view_users)
        usersRecyclerView = view.findViewById(R.id.users_recycler_view)

        usersRecyclerView.layoutManager = LinearLayoutManager(this)
        // Use the filtered list for display
        usersAdapter = AdapterUsers(filteredUsersList) { userDoc ->
            openUserProfile(userDoc.id)
        }
        usersRecyclerView.adapter = usersAdapter

        loadAllUsers()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterUsers(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterUsers(newText)
                return true
            }
        })
    }

    private fun loadAllUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                usersList.clear()
                filteredUsersList.clear()
                if (!querySnapshot.isEmpty) {
                    usersList.addAll(querySnapshot.documents)
                    filteredUsersList.addAll(usersList)
                }
                usersAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { ex ->
                toast("Failed to load users: ${ex.message}")
            }
    }

    private fun filterUsers(query: String?) {
        val q = query?.trim() ?: ""
        filteredUsersList.clear()
        if (q.isEmpty()) {
            filteredUsersList.addAll(usersList)
        } else {
            filteredUsersList.addAll(
                usersList.filter { doc ->
                    val displayName = doc.getString("displayName") ?: ""
                    displayName.contains(q, ignoreCase = true)
                }
            )
        }
        usersAdapter.notifyDataSetChanged()
    }

    //--------------------------------------------------------------------------
    // Navigation Helpers
    //--------------------------------------------------------------------------
    /** Called by AdapterRooms and AdapterEvents when a user taps on a room */
    override fun openRoom(roomId: String) {
        toast("Open Room: $roomId")
        // startActivity(Intent(...))
    }

    /** Called by AdapterUsers when a user taps on a user */
    private fun openUserProfile(userId: String) {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("USER_ID_KEY", userId)
        startActivity(intent)
    }
}
