package com.example.otofuda_android.Result

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.otofuda_android.R
import com.example.otofuda_android.Response.Music

class CustomAdapter(private val musicList: ArrayList<Music>): RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    lateinit var listener: OnItemClickListener
    var viewWidth = 0
    var viewHeight = 0

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val number: TextView = view.findViewById(R.id.number_text_view)
        val title: TextView = view.findViewById(R.id.title_text_view)
        val artist: TextView = view.findViewById(R.id.artist_text_view)
        var badgeButton: Button = view.findViewById(R.id.badge_button)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.result_list_item, viewGroup, false)
//        viewWidth = viewGroup.getMeasuredWidth() / 4;
//        viewHeight = viewGroup.getMeasuredHeight() / 4;
//        view.layoutParams.width = viewWidth
//        view.layoutParams.height = viewHeight
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val music = musicList[position]
        viewHolder.number.text = (position+1).toString()
        viewHolder.title.text = music.title
        viewHolder.artist.text = music.artist
        viewHolder.view.setOnClickListener {
            listener.onItemClickListener(it, position, musicList[position].title)
        }
        viewHolder.badgeButton.setOnClickListener {
            listener.onBadgeClickListener(it, position, musicList[position].title)
        }

    }

    override fun getItemCount() = musicList.size

    interface OnItemClickListener{
        fun onItemClickListener(view: View, position: Int, clickedText: String)
        fun onBadgeClickListener(view: View, position: Int, clickedText: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }
}