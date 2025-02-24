/**
 * ExplorerActivity allows users to browse and search for available rooms and users.
 *
 * This activity uses a ViewPager2 to display two pages:
 * 1. Explorer page – where users can search for rooms and users via a SearchView.
 * 2. Rooms page – which displays event rooms (rooms created by the user).
 *
 * The activity implements two tabs on the explorer page:
 * - "Users" tab: displays a list of users using AdapterUsers.
 * - "Rooms" tab: displays a list of active rooms using AdapterRooms.
 *
 * Key functionalities include:
 * - Loading rooms and users from Firestore.
 * - Filtering results based on search criteria.
 * - Toggling between user and room views.
 *
 * Adapters:
 * - AdapterRooms: Displays room items.
 * - AdapterUsers: Displays user items.
 *
*/
package com.example.bettertogether

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import android.content.Context
import com.bumptech.glide.Glide

class ExplorerActivity : BaseActivity() {

    // List of full document snapshots from Firestore.
    private val docList = mutableListOf<DocumentSnapshot>()
    // List of documents filtered by search criteria.
    private val filteredDocList = mutableListOf<DocumentSnapshot>()

    // UI components for the explorer view.
    private lateinit var recyclerView: RecyclerView
    private lateinit var roomsAdapter: AdapterRooms
    private lateinit var usersAdapter: AdapterUsers
    private lateinit var tabUsers: TextView
    private lateinit var tabRooms: TextView
    private lateinit var indicator: View

    // UI components for the Rooms view.
    private lateinit var yourRoomsAdapter: AdapterEvents
    private lateinit var tabMyRooms: TextView
    private lateinit var tabMyFriends: TextView
    private lateinit var indicatorMy: View

    private var isUsersTabActive = false

    /**
     * onCreate initializes the activity by setting the content view and configuring the ViewPager2.
     * It registers a callback to initialize the explorer or rooms view depending on the page selected.
     */
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

    /**
     * initExplorerView sets up the explorer page.
     *
     * It configures the RecyclerView with AdapterRooms and AdapterUsers, sets up the tabs,
     * and attaches a SearchView listener to filter the documents based on user input.
     */
    private fun initExplorerView(view: View) {
        recyclerView = view.findViewById(R.id.explorer_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        roomsAdapter = AdapterRooms(filteredDocList){ document -> openRoom(document.id) }
        usersAdapter = AdapterUsers(filteredDocList){ document -> openUser(document.id) }
        recyclerView.adapter = roomsAdapter

        tabUsers = view.findViewById(R.id.tab_users)
        tabRooms = view.findViewById(R.id.tab_rooms)
        indicator = view.findViewById(R.id.indicator)
        activateTabRooms()
        tabUsers.setOnClickListener { activateTabUsers() }
        tabRooms.setOnClickListener { activateTabRooms() }

        val searchView = view.findViewById<SearchView>(R.id.search_view)
        searchView.isIconified = false
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterDocs(query)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterDocs(newText)
                return true
            }
        })
    }

    /**
     * activateTabUsers updates the UI for the "Users" tab.
     * It sets the appropriate text colors and indicator animation, switches the RecyclerView adapter,
     * and loads all users from Firestore.
     */
    private fun activateTabUsers() {
        isUsersTabActive = true
        tabUsers.setTextColor(resources.getColor(android.R.color.white, null))
        tabRooms.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        indicator.layoutParams.width = tabRooms.width
        indicator.requestLayout()
        indicator.animate().translationX(tabUsers.width.toFloat()).setDuration(400).start()

        recyclerView.adapter = usersAdapter
        loadAllUsers()
    }

    /**
     * activateTabRooms updates the UI for the "Rooms" tab.
     * It adjusts text colors and indicator animation, sets the adapter for rooms,
     * and loads all active rooms from Firestore.
     */
    private fun activateTabRooms() {
        isUsersTabActive = false
        tabRooms.setTextColor(resources.getColor(android.R.color.white, null))
        tabUsers.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        indicator.layoutParams.width = tabUsers.width
        indicator.requestLayout()
        indicator.animate().translationX(0f).setDuration(400).start()

        recyclerView.adapter = roomsAdapter
        loadAllRooms()
    }

    /**
     * loadAllRooms queries Firestore for active rooms.
     * It clears the current document list, adds the new documents, updates the filtered list,
     * and notifies the AdapterRooms adapter.
     */
    private fun loadAllRooms() {
        db.collection("rooms")
            .whereEqualTo("isActive", true)
            .get()
            .addOnSuccessListener { querySnapshot ->
                docList.clear()
                docList.addAll(querySnapshot.documents)
                filteredDocList.clear()
                filteredDocList.addAll(docList)
                roomsAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                toast("Error fetching rooms: ${exception.message}")
            }
    }

    /**
     * loadAllUsers queries Firestore for all users.
     * It updates the document lists and notifies the AdapterUsers adapter.
     */
    private fun loadAllUsers() {
        db.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                docList.clear()
                docList.addAll(querySnapshot.documents)
                filteredDocList.clear()
                filteredDocList.addAll(docList)
                usersAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                toast("Error fetching users: ${exception.message}")
            }
    }

    /**
     * filterDocs filters the complete document list based on the provided search query.
     * It updates the filteredDocList and notifies the current adapter.
     *
     * @param query The search query input by the user.
     */
    private fun filterDocs(query: String?) {
        val searchQuery = query?.trim() ?: ""
        filteredDocList.clear()

        if (searchQuery.isEmpty()) {
            filteredDocList.addAll(docList)
        } else {
            filteredDocList.addAll(
                docList.filter { document ->
                    val name = document.getString("name")
                        ?: document.getString("displayName")
                        ?: "Unnamed"
                    name.contains(searchQuery, ignoreCase = true)
                }
            )
        }
        recyclerView.adapter?.notifyDataSetChanged()
    }

    /**
     * initRoomsView sets up the Rooms page in the ViewPager.
     *
     * It configures the RecyclerView with AdapterEvents and AdapterUsers,
     * and initializes tabs for "My Rooms" and "My Friends".
     */
    private fun initRoomsView(view: View) {
        recyclerView = view.findViewById(R.id.rooms_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        yourRoomsAdapter = AdapterEvents(docList) { document -> openRoom(document.id) }
        usersAdapter = AdapterUsers(docList) { document -> openUser(document.id) }
        recyclerView.adapter = yourRoomsAdapter

        tabMyRooms = view.findViewById(R.id.tab_my_rooms)
        tabMyFriends = view.findViewById(R.id.tab_my_friends)
        indicatorMy = view.findViewById(R.id.indicator_rooms)

        activateTabMyRooms()

        tabMyRooms.setOnClickListener { activateTabMyRooms() }
        tabMyFriends.setOnClickListener { activateTabMyFriends() }
    }

    /**
     * activateTabMyRooms activates the "My Rooms" tab.
     *
     * It updates the UI styling, switches the adapter to display events,
     * and triggers loading of the user's rooms.
     */
    private fun activateTabMyRooms() {
        isUsersTabActive = false
        tabMyRooms.setTextColor(resources.getColor(android.R.color.white, null))
        tabMyFriends.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        indicatorMy.layoutParams.width = tabMyFriends.width
        indicatorMy.requestLayout()
        indicatorMy.animate().translationX(0f).setDuration(400).start()

        recyclerView.adapter = yourRoomsAdapter
        loadMyRooms(docList, yourRoomsAdapter)
    }

    /**
     * activateTabMyFriends activates the "My Friends" tab.
     *
     * It updates the UI styling, switches the adapter to display users,
     * and loads the current user's friends.
     */
    private fun activateTabMyFriends() {
        isUsersTabActive = true
        tabMyFriends.setTextColor(resources.getColor(android.R.color.white, null))
        tabMyRooms.setTextColor(resources.getColor(android.R.color.darker_gray, null))
        indicatorMy.layoutParams.width = tabMyRooms.width
        indicatorMy.requestLayout()
        indicatorMy.animate().translationX(tabMyFriends.width.toFloat()).setDuration(400).start()

        recyclerView.adapter = usersAdapter
        loadMyFriends(docList, usersAdapter)
    }

    /**
     * loadMyFriends loads the current user's friends from Firestore.
     *
     * It retrieves the current user's document, extracts the active friends list,
     * and then queries Firestore to fetch detailed friend profiles.
     *
     * @param docList The list to update with friend documents.
     * @param usersAdapter The adapter to notify once data is loaded.
     */
    protected fun loadMyFriends(docList: MutableList<DocumentSnapshot>, usersAdapter: AdapterUsers) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    toast("User not found.")
                    return@addOnSuccessListener
                }

                val friends = getUserActiveFriends(document)
                val friendIds = friends.keys.toList()

                if (friendIds.isEmpty()) {
                    docList.clear()
                    usersAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }
                // Fetch friends from Firestore based on friend IDs.
                db.collection("users")
                    .whereIn(FieldPath.documentId(), friendIds)
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        docList.clear()
                        docList.addAll(querySnapshot.documents)
                        usersAdapter.notifyDataSetChanged()
                    }
                    .addOnFailureListener { exception ->
                        toast("Error fetching friends: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                toast("Error fetching user data: ${exception.message}")
            }
    }
}

// Adapter for displaying Users
class AdapterUsers(
    private val participants: List<DocumentSnapshot>,
    private val onUserClick: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<AdapterUsers.UserViewHolder>() {
    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = itemView.findViewById(R.id.participantName)
        val roleTextView: TextView = itemView.findViewById(R.id.participantRole)
        val profileImageView: ImageView = itemView.findViewById(R.id.participantImage)
        val pointsTextView: TextView = itemView.findViewById(R.id.participantPoints)
        val rankTextView: TextView = itemView.findViewById(R.id.participantRank)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_participant, parent, false)
        return UserViewHolder(view)
    }
    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val document = participants[position]
        val name = document.getString("displayName") ?: "Unknown"
        val role = document.getString("role") ?: "No Role"
        val imageUrl = document.getString("photoUrl") ?: ""
        val points = document.getLong("currentPoints") ?: 0L

        holder.nameTextView.text = name
        holder.roleTextView.text = role
        holder.pointsTextView.text = "Points: $points"
        Glide.with(holder.profileImageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(holder.profileImageView)
        holder.itemView.setOnClickListener {
            onUserClick(document)
        }
        holder.rankTextView.text = (position + 1).toString()
    }
    override fun getItemCount(): Int = participants.size
}

// Adapter for displaying Rooms
class AdapterRooms(
    private val rooms: List<DocumentSnapshot>,
    private val onRoomClick: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<AdapterRooms.RoomViewHolder>() {
    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomNameTextView: TextView = view.findViewById(R.id.room_name)
        val participantsCounterTextView: TextView = view.findViewById(R.id.participants_count)
        val lockIconImageView: ImageView = view.findViewById(R.id.lock_icon)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }
    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val roomDocument = rooms[position]
        val roomName = roomDocument.getString("name") ?: "Unnamed Room"
        var roomsParticipants = getActiveParticipants(roomDocument)
        val isPublic = roomDocument.getBoolean("isPublic") ?: false
        val maxParticipants = roomDocument.getLong("maxParticipants")?.toInt() ?: 10

        holder.roomNameTextView.text = roomName
        holder.participantsCounterTextView.text = "${roomsParticipants.size}/$maxParticipants"
        holder.lockIconImageView.visibility = if (isPublic) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            onRoomClick(roomDocument)
        }
    }
    override fun getItemCount(): Int = rooms.size
    protected fun getActiveParticipants(roomDoc: DocumentSnapshot): Map<String, Map<String, Any>> {
        val roomsParticipants = roomDoc.get("participants") as? Map<String, Map<String, Any>> ?: emptyMap()
        return roomsParticipants.filterValues { it["isActive"] == true && it["role"]!="banned" }
    }
}

// Adapter for handling pages (Explorer and Rooms)
class AdapterRoomsPager(private val context: Context) : RecyclerView.Adapter<AdapterRoomsPager.ViewHolder>() {
    private val pageViews = mutableMapOf<Int, View>() // Save views by position

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val layout = when (viewType) {
            0 -> R.layout.page_explorer
            1 -> R.layout.page_rooms
            else -> throw IllegalStateException("Invalid view type")
        }
        val view = inflater.inflate(layout, parent, false)
        return ViewHolder(view)
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        pageViews[position] = holder.itemView // Save the view in map
    }
    fun getPageView(position: Int): View? = pageViews[position] // Access page by position
    override fun getItemViewType(position: Int): Int = position // Determine page type by position
    override fun getItemCount(): Int = 2 // Only two pages
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
