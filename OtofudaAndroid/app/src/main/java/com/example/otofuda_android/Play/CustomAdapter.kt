package com.example.otofuda_android.Play

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.otofuda_android.R
import com.example.otofuda_android.Response.Music

class CustomAdapter(private val musicList: ArrayList<Music>): RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    lateinit var listener: OnItemClickListener
    var viewWidth = 0
    var viewHeight = 0
    var colmunCount = 0
    var rowCount = 0
    var viewGroup: ViewGroup? = null

    fun setColmunRow(column: Int, row: Int){
        colmunCount = column
        rowCount = row
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.text_view)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context).inflate(R.layout.list_item, viewGroup, false)
        viewWidth = viewGroup.getMeasuredWidth() / colmunCount
        viewHeight = viewGroup.getMeasuredHeight() / rowCount
        view.layoutParams.width = viewWidth
        view.layoutParams.height = viewHeight
        this.viewGroup = viewGroup
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val music = musicList[position]
        viewHolder.name.text = music.name
        viewHolder.view.setOnClickListener {
            listener.onItemClickListener(it, position, music.name!!)
        }
    }

    override fun getItemCount() = musicList.size

    interface OnItemClickListener{
        fun onItemClickListener(view: View, position: Int, clickedText: String)
    }

    fun setOnItemClickListener(listener: OnItemClickListener){
        this.listener = listener
    }

    fun setOwner(position: Int, color: Int){
        val rotate = RotateAnimation(
            0.0f, 360.0f * 5,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f
        );

        // animation時間 msec
        rotate.setDuration(2000);
        // animationが終わったそのまま表示にする
        rotate.setFillAfter(true);

        val view = viewGroup!!.getChildAt(position)
        view.startAnimation(rotate)

        val textView = view.findViewById(R.id.text_view) as TextView
        textView.setBackgroundColor(color)
        textView.setTextColor((Color.WHITE))
    }
}