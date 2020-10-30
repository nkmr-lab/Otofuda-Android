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
import kotlin.collections.ArrayList

class ResultVC : AppCompatActivity() {

    var playMusics = ArrayList<Music>()
    var orderMusics = ArrayList<Music>()
    var score = 0

    var mediaPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result)

        supportActionBar?.title = "結果"

        playMusics = intent.getSerializableExtra("playMusics") as ArrayList<Music>
        score = intent.getIntExtra("score", 0)

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
                val url = playMusics[position].preview_url
                mediaPlayer = MediaPlayer().apply {
                    setAudioStreamType(AudioManager.STREAM_MUSIC)
                    setDataSource(url)
                    prepare() // might take long! (for buffering, etc)
                    start()
                }
            }

            override fun onBadgeClickListener(view: View, position: Int, clickedText: String) {
                val playMusic = playMusics[position]
                val storeURL = playMusic.store_url
                val uri = Uri.parse(storeURL)
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }


        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onRestartButtonTapped(view: View?){
        val intent = Intent(this, MenuVC::class.java)
        startActivity(intent)
    }
}