package com.example.bettertogether

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.DocumentSnapshot

class AdapterUsers(
    private var users: MutableList<DocumentSnapshot>,
    private val onUserClick: (DocumentSnapshot) -> Unit
) : RecyclerView.Adapter<AdapterUsers.UserViewHolder>() {

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val userNameTextView: TextView = view.findViewById(R.id.user_name)
        val userProfileImageView: ImageView = view.findViewById(R.id.user_profile_image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val userDocument = users[position]
        val userName = userDocument.getString("displayName") ?: "Unnamed User"
        holder.userNameTextView.text = userName

        val imageUrl = userDocument.getString("photoUrl") ?: ""
        Glide.with(holder.userProfileImageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_profile)
            .error(R.drawable.ic_profile)
            .into(holder.userProfileImageView)

        holder.itemView.setOnClickListener {
            onUserClick(userDocument)
        }
    }

    override fun getItemCount(): Int = users.size

    fun updateData(newUsers: List<DocumentSnapshot>) {
        users.clear()
        users.addAll(newUsers)
        notifyDataSetChanged()
    }
}
