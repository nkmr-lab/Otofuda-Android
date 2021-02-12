package com.example.otofuda_android.Result

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.otofuda_android.Menu.MenuVC
import com.example.otofuda_android.R
import com.example.otofuda_android.Response.Music
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlin.collections.ArrayList

class ResultVC : AppCompatActivity() {

    var playMusics = ArrayList<Music>()
    var orderMusics = ArrayList<Music>()
    var score = 0

    var mediaPlayer: MediaPlayer? = null

    var uuid = ""

    var roomId = ""

    var memberId = 0

    val database = Firebase.database


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result)

        supportActionBar?.title = "結果"

        playMusics = intent.getSerializableExtra("playMusics") as ArrayList<Music>
        score = intent.getIntExtra("score", 0)
        uuid = intent.getStringExtra("uuid")
        roomId = intent.getStringExtra("roomId")
        memberId = intent.extras.getInt("memberId")

        var scoreLabel = this.findViewById(R.id.scoreLabel) as TextView
        scoreLabel.text = score.toString() + "点"

        println(playMusics)

        val recycler_view = this.findViewById(R.id.result_recycler_view) as RecyclerView

        val customAdapter = CustomAdapter(playMusics)
        recycler_view.adapter = customAdapter
        recycler_view.layoutManager = GridLayoutManager(this, 1, RecyclerView.VERTICAL, false)

        customAdapter.setOnItemClickListener(object: CustomAdapter.OnItemClickListener {
            override fun onItemClickListener(view: View, position: Int, clickedText: String) {
                if( mediaPlayer != null ){
                    mediaPlayer!!.stop()
                }
                val url = playMusics[position].previewURL
                mediaPlayer = MediaPlayer().apply {
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                    setDataSource(url)
                    isLooping = true
                    prepare() // might take long! (for buffering, etc)
                    start()
                }
            }

            override fun onBadgeClickListener(view: View, position: Int, clickedText: String) {
                val playMusic = playMusics[position]
                val storeURL = playMusic.storeURL
                val uri = Uri.parse(storeURL)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }


        })
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onRestartButtonTapped(view: View?){
        var statusRef = database.getReference("rooms/" + roomId + "/status")
        statusRef.setValue("menu")

        val intent = Intent(this, MenuVC::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("roomId", roomId)
        intent.putExtra("memberId", memberId)
        intent.putExtra( "uuid", uuid )
        startActivity(intent)
    }
}