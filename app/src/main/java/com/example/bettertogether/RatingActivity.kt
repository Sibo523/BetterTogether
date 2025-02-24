package com.example.bettertogether

import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.DocumentSnapshot
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar  // Added missing import
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore

class RatingActivity : BaseActivity() {

    // RecyclerView to display the leaderboard.
    private lateinit var recyclerView: RecyclerView
    // Adapter that binds participant data to the RecyclerView.
    private lateinit var adapter: AdapterParticipants
    // List to hold the top user documents fetched from Firestore.
    private val topUsersList = mutableListOf<DocumentSnapshot>()

    /**
     * Called when the activity is created.
     *
     * Sets the content view, initializes the RecyclerView and its adapter, and triggers the loading of top users.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down, this Bundle contains the data it most recently supplied; otherwise, it is null.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        recyclerView = findViewById(R.id.rating_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdapterParticipants(topUsersList) { document ->
            openUser(document.id)
        }
        recyclerView.adapter = adapter

        loadTopUsers()
    }

    /**
     * loadTopUsers queries Firestore to retrieve the top 20 users ordered by currentPoints in descending order.
     *
     * On success:
     * - Clears the current [topUsersList]
     * - Adds the fetched documents
     * - Notifies the adapter to update the RecyclerView
     *
     * On failure:
     * - Displays a toast message with the error.
     */
    private fun loadTopUsers() {
        db.collection("users")
            .orderBy("currentPoints", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(20)
            .get()
            .addOnSuccessListener { documents ->
                topUsersList.clear()
                topUsersList.addAll(documents.documents)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                // Use the BaseActivity toast() method to display error message.
                toast("Error fetching top users: ${exception.message}")
            }
    }
}

/**
 * AdapterParticipants binds a list of user documents to a RecyclerView.
 *
 * Each item displays:
 * - User's display name
 * - User's role
 * - Profile image (loaded using Glide)
 * - Current points
 * - Rank (based on position in the list)
 *
 * When an item is clicked, [onUserClick] is invoked with the corresponding DocumentSnapshot.
 *
 * @param participants List of user DocumentSnapshots.
 * @param onUserClick Lambda function to handle click events on a user item.
 */
class AdapterParticipants(
    private val participants: List<DocumentSnapshot>,
    private val onUserClick: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<AdapterParticipants.ParticipantViewHolder>() {

    /**
     * ParticipantViewHolder holds references to the views for each user item in the leaderboard.
     *
     * Views include:
     * - nameTextView: Displays the user's display name.
     * - roleTextView: Displays the user's role.
     * - profileImageView: Displays the user's profile image.
     * - pointsTextView: Displays the user's current points.
     * - rankTextView: Displays the user's rank.
     */
    class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.participantName)
        val roleTextView: TextView = itemView.findViewById(R.id.participantRole)
        val profileImageView: ImageView = itemView.findViewById(R.id.participantImage)
        val pointsTextView: TextView = itemView.findViewById(R.id.participantPoints)
        val rankTextView: TextView = itemView.findViewById(R.id.participantRank)
    }

    /**
     * onCreateViewHolder inflates the layout for a user item and returns a ParticipantViewHolder.
     *
     * @param parent The parent ViewGroup.
     * @param viewType The view type of the new View.
     * @return A new instance of ParticipantViewHolder.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_participant, parent, false)
        return ParticipantViewHolder(view)
    }

    /**
     * onBindViewHolder binds data from a user DocumentSnapshot to the corresponding view holder.
     *
     * It retrieves the user's display name, role, profile image URL, and current points from the document,
     * sets the respective views, and assigns an OnClickListener that triggers [onUserClick].
     * Additionally, it assigns the rank (position + 1) to the rankTextView.
     *
     * @param holder The ParticipantViewHolder instance.
     * @param position The position of the item in the list.
     */
    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val document = participants[position]

        // Fetch data from the DocumentSnapshot.
        val name = document.getString("displayName") ?: "Unknown"
        val role = document.getString("role") ?: "No Role"
        val imageUrl = document.getString("photoUrl") ?: ""
        val points = document.getLong("currentPoints") ?: 0L

        // Set text views with the fetched data.
        holder.nameTextView.text = name
        holder.roleTextView.text = role
        holder.pointsTextView.text = "Points: $points"

        // Load profile image using Glide.
        Glide.with(holder.profileImageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(holder.profileImageView)

        // Set click listener to handle user clicks.
        holder.itemView.setOnClickListener {
            onUserClick(document)
        }
        // Set rank text (position + 1).
        holder.rankTextView.text = (position + 1).toString()
    }

    /**
     * getItemCount returns the total number of user items.
     *
     * @return The size of the participants list.
     */
    override fun getItemCount(): Int = participants.size
}