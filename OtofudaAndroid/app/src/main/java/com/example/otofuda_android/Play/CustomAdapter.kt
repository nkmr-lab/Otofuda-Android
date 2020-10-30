package com.example.otofuda_android.Play

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.otofuda_android.R
import com.example.otofuda_android.Response.Music

class CustomAdapter(private val musicList: ArrayList<Music>): RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    lateinit var listener: OnItemClickListener
    var viewWidth = 0
    var viewHeight = 0

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.text_view)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_item, viewGroup, false)
        viewWidth = viewGroup.getMeasuredWidth() / 4;
        viewHeight = viewGroup.getMeasuredHeight() / 4;
        view.layoutParams.width = viewWidth
        view.layoutParams.height = viewHeight
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val music = musicList[position]
        viewHolder.name.text = music.title
        viewHolder.view.setOnClickListener {
            listener.onItemClickListener(it, position, musicList[position].title)
        }
    }

    override fun getItemCount() = musicList.size

    interface OnItemClickListener{
        fun onItemClickListener(view: View, position: Int, clickedText: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }
}