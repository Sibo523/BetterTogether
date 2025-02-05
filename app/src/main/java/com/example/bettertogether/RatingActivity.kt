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

    // Ensure 'db' is defined in your BaseActivity or initialize it here:
    // private val db = FirebaseFirestore.getInstance()

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AdapterParticipants
    private val topUsersList = mutableListOf<DocumentSnapshot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        recyclerView = findViewById(R.id.rating_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AdapterParticipants(topUsersList)
        recyclerView.adapter = adapter

        loadTopUsers()
    }

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
                // Assuming toast() is defined in BaseActivity. Otherwise, use:
                // Toast.makeText(this, "Error fetching top users: ${exception.message}", Toast.LENGTH_SHORT).show()
                toast("Error fetching top users: ${exception.message}")
            }
    }
}

class AdapterParticipants(private val participants: List<DocumentSnapshot>) :
    RecyclerView.Adapter<AdapterParticipants.ParticipantViewHolder>() {

    class ParticipantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.participantName)
        val roleTextView: TextView = itemView.findViewById(R.id.participantRole)
        val profileImageView: ImageView = itemView.findViewById(R.id.participantImage)
        val pointsTextView: TextView = itemView.findViewById(R.id.participantPoints)

        // NEW: Rank TextView
        val rankTextView: TextView = itemView.findViewById(R.id.participantRank)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_top_participant, parent, false)
        return ParticipantViewHolder(view)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val document = participants[position]

        // Fetch data from Firestore
        val name = document.getString("displayName") ?: "Unknown"
        val role = document.getString("role") ?: "No Role"
        val imageUrl = document.getString("photoUrl") ?: ""
        val points = document.getLong("currentPoints") ?: 0L

        // Set data to the views
        holder.nameTextView.text = name
        holder.roleTextView.text = role
        holder.pointsTextView.text = "Points: $points"

        // Load profile image with Glide (if needed)
        Glide.with(holder.profileImageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(holder.profileImageView)

        // Assign rank (position + 1) to the rank TextView
        holder.rankTextView.text = (position + 1).toString()
    }

    override fun getItemCount(): Int = participants.size
}
