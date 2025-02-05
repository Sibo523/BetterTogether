package com.example.bettertogether

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath

import android.content.Context

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
            .whereEqualTo("isActive", true)
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
            return
        }
        db.collection("users").document(user.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    roomsList.clear()
                    val roomIds = getUserActiveRooms(document)
                    if (roomIds.size > 0) {
                        val ids = roomIds.mapNotNull { it["roomId"] as? String }
                        db.collection("rooms")
                            .whereIn(FieldPath.documentId(), ids)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                roomsList.addAll(querySnapshot.documents)
                                roomsAdapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener{ exception -> toast("Error fetching rooms: ${exception.message}") }
                    }
                }
            }
            .addOnFailureListener{ exception -> toast("Error fetching user data: ${exception.message}") }
    }
}


class AdapterRooms(
    private val rooms: List<DocumentSnapshot>, // List of Firestore document snapshots
    private val onRoomClick: (DocumentSnapshot) -> Unit // Callback for handling room clicks
) : RecyclerView.Adapter<AdapterRooms.RoomViewHolder>() {
    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val roomNameTextView: TextView = view.findViewById(R.id.room_name)
        val participantsCounterTextView: TextView = view.findViewById(R.id.participants_counter)
        val lockIconImageView: ImageView = view.findViewById(R.id.lock_icon)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }
    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val roomDocument = rooms[position]
        val roomName = roomDocument.getString("name") ?: "Unnamed Room"
        var roomsParticipants = roomDocument.get("participants") as? Map<String, Map<String, Any>> ?: emptyMap()
        roomsParticipants = roomsParticipants.filterValues { it["isActive"] == true }
        val isPublic = roomDocument.getBoolean("isPublic") ?: false
        val maxParticipants = roomDocument.getLong("maxParticipants")?.toInt() ?: 10

        holder.roomNameTextView.text = roomName
        holder.participantsCounterTextView.text = "${roomsParticipants.size}/$maxParticipants"
        holder.lockIconImageView.visibility = if (isPublic) View.GONE else View.VISIBLE

        holder.itemView.setOnClickListener {
            onRoomClick(roomDocument) // Pass the full document snapshot to the callback
        }
    }
    override fun getItemCount(): Int {
        return rooms.size
    }
}


class AdapterRoomsPager(private val context: Context) : RecyclerView.Adapter<AdapterRoomsPager.ViewHolder>() {
    private val pageViews = mutableMapOf<Int, View>() // שמירה על Views לפי position

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
        pageViews[position] = holder.itemView // שמירת ה-View במפה
    }
    fun getPageView(position: Int): View? = pageViews[position] // גישה לעמוד לפי position
    override fun getItemViewType(position: Int): Int = position // קביעת סוג העמוד
    override fun getItemCount(): Int = 2 // שני עמודים בלבד
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
