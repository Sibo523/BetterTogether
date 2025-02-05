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

    private lateinit var recyclerView: RecyclerView
    private lateinit var roomsAdapter: AdapterRooms
    private lateinit var yourRoomsAdapter: AdapterEvents

    // For "Users" tab
    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var usersAdapter: AdapterUsers

    // These are the "All Rooms" data (for the Explorer tab, page 0)
    private val roomsList = mutableListOf<DocumentSnapshot>()        // Full room list
    private val filteredRoomsList = mutableListOf<DocumentSnapshot>() // Filtered subset

    // These are for the "Users" tab, page 2
    private val usersList = mutableListOf<DocumentSnapshot>()
    // If you want user-search functionality, you could also do a filteredUsersList.

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
                        0 -> initExplorerView(currentView) // "All Rooms"
                        1 -> initMyRoomsView(currentView)   // "Your Rooms"
                        2 -> initUsersView(currentView)     // "Users"
                    }
                }
            }
        })
    }

    // -------------------------------------------------------------------
    // Page 0: Explorer (All Rooms)
    // -------------------------------------------------------------------
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

    /**
     *  Fetch *all* rooms from Firestore and store them in roomsList + filteredRoomsList
     */
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

                // Populate the lists
                roomsList.addAll(querySnapshot.documents)
                // By default, show everything in the "filteredRoomsList"
                filteredRoomsList.addAll(roomsList)

                // Notify the adapter
                roomsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                toast("Error fetching rooms: ${exception.message}")
            }
    }

    /**
     * Filter the rooms based on the query string
     */
    private fun filterRooms(query: String?) {
        val searchQuery = query?.trim() ?: ""
        filteredRoomsList.clear()

        if (searchQuery.isEmpty()) {
            // Show all rooms
            filteredRoomsList.addAll(roomsList)
        } else {
            // Show only matching rooms
            val filtered = roomsList.filter { doc ->
                val roomName = doc.getString("name") ?: "Unnamed Room"
                roomName.contains(searchQuery, ignoreCase = true)
            }
            filteredRoomsList.addAll(filtered)
        }

        roomsAdapter.notifyDataSetChanged()
    }

    // -------------------------------------------------------------------
    // Page 1: "Your Rooms" (rooms for the current user)
    // -------------------------------------------------------------------
    private fun initMyRoomsView(view: View) {
        // Recycle the same variable 'recyclerView' or create a separate one
        recyclerView = view.findViewById(R.id.rooms_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // This adapter is for the current user's rooms
        yourRoomsAdapter = AdapterEvents(roomsList) { document ->
            openRoom(document.id)
        }
        recyclerView.adapter = yourRoomsAdapter

        loadUserRooms()
    }

    /**
     * Load only the current user's rooms (by reading from user doc, then rooms collection).
     */
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
                    roomsList.clear() // Reuse roomsList for "my rooms"
                    val roomIds = getUserActiveRooms(document) // Your custom function

                    if (roomIds.isNotEmpty()) {
                        val ids = roomIds.mapNotNull { it["roomId"] as? String }

                        db.collection("rooms")
                            .whereIn(FieldPath.documentId(), ids)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                roomsList.addAll(querySnapshot.documents)
                                yourRoomsAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener { exception ->
                                toast("Error fetching rooms: ${exception.message}")
                            }
                    } else {
                        // If user has no rooms, maybe notify the adapter now
                        yourRoomsAdapter.notifyDataSetChanged()
                    }
                } else {
                    toast("No user data found.")
                }
            }
            .addOnFailureListener { exception ->
                toast("Error fetching user data: ${exception.message}")
            }
    }

    // -------------------------------------------------------------------
    // Page 2: "Users" (all users from Firestore)
    // -------------------------------------------------------------------
    private fun initUsersView(view: View) {
        // Set up RecyclerView
        usersRecyclerView = view.findViewById(R.id.users_recycler_view)
        usersRecyclerView.layoutManager = LinearLayoutManager(this)

        // Create the adapter
        usersAdapter = AdapterUsers(usersList) { userDocument ->
            // This callback is triggered when user item is clicked
            openUserProfile(userDocument.id)
        }
        usersRecyclerView.adapter = usersAdapter

        // Load all users from Firestore
        loadAllUsers()
    }

    /**
     * Query Firestore for all users and update our 'usersList'
     */
    private fun loadAllUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                usersList.clear()

                if (querySnapshot.isEmpty) {
                    toast("No users found")
                    usersAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                usersList.addAll(querySnapshot.documents)
                usersAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                toast("Error fetching users: ${exception.message}")
            }
    }

    // -------------------------------------------------------------------
    // Navigation Helpers
    // -------------------------------------------------------------------
    /**
     * Called by AdapterRooms and AdapterEvents when a user taps on a room
     */
    override fun openRoom(roomId: String) {
        // Example: open your chat or room activity
        toast("Open Room: $roomId")
        // startActivity(Intent(...))
    }

    /**
     * Called by AdapterUsers when a user taps on a user
     */
    private fun openUserProfile(userId: String) {
        // Launch ProfileActivity and pass that user’s ID
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("USER_ID_KEY", userId)
        startActivity(intent)
    }
}
