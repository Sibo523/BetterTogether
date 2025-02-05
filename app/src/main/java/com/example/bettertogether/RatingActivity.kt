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
import android.widget.TextView
import com.bumptech.glide.Glide

class RatingActivity : BaseActivity() {

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
                topUsersList.addAll(documents.documents) // שמירה של כל הדוקומנט
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
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
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_top_participant, parent, false)
        return ParticipantViewHolder(view)
    }
    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        val document = participants[position]
        val name = document.getString("displayName") ?: "Unknown"
        val role = document.getString("role") ?: "No Role"
        val imageUrl = document.getString("photoUrl") ?: ""
        holder.nameTextView.text = name
        holder.roleTextView.text = role
        Glide.with(holder.profileImageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(holder.profileImageView)
    }
    override fun getItemCount(): Int = participants.size
}