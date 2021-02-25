package com.snakamura.otofuda_android.Result

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.snakamura.otofuda_android.Menu.MenuVC
import com.snakamura.otofuda_android.R
import com.snakamura.otofuda_android.Response.Music
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
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

    var memberCount = 0

    val database = Firebase.database

    var statusListener: ValueEventListener? = null

    var cardCount = 0
    var cardColumnCount = 0
    var cardRowCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.result)

        supportActionBar?.title = "結果"

        playMusics = intent.getSerializableExtra("playMusics") as ArrayList<Music>
        score = intent.getIntExtra("score", 0)
        uuid = intent.getStringExtra("uuid")
        roomId = intent.getStringExtra("roomId")
        memberId = intent.getIntExtra("memberId", 0)
        memberCount = intent.getIntExtra("memberCount", 0)
        cardCount = intent.extras.getInt("cardCount")
        cardColumnCount = intent.extras.getInt("cardColumnCount")
        cardRowCount = intent.extras.getInt("cardRowCount")

        var winnerLabel = this.findViewById(R.id.winnerLabel) as TextView

        var eachScores = IntArray(memberCount)
        for ( music in playMusics ){

            // 誰も取ってないカードはカウントしない
            if( music.cardOwner != -1 ) {
                eachScores[music.cardOwner!!] += 1
            }
        }

        if ( eachScores[memberId] == eachScores.max() ){
            if ( eachScores.filter { it == eachScores.max() }.size > 1 ){
                winnerLabel.text = "引き分け"
            } else {
                winnerLabel.text = "あなたの勝利"
            }
        } else {
            winnerLabel.text = "あなたの敗北"
        }


        var scoreLabel = this.findViewById(R.id.scoreLabel) as TextView
        scoreLabel.text = eachScores[memberId].toString() + "点"

        val retryButton = this.findViewById(R.id.retryButton) as Button
        if( memberId != 0 ){
            retryButton.visibility = Button.GONE
        }

        val recyclerView = this.findViewById(R.id.result_recycler_view) as RecyclerView

        val customAdapter = CustomAdapter(playMusics)
        recyclerView.adapter = customAdapter
        recyclerView.layoutManager = GridLayoutManager(this, 1, RecyclerView.VERTICAL, false)

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

        observeRoomStatus()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onRestartButtonTapped(view: View?){
        var statusRef = database.getReference("rooms/" + roomId + "/status")
        statusRef.setValue("menu")
    }

    private fun observeRoomStatus() {
        var statusRef = database.getReference("rooms/" + roomId + "/status")

        statusListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val status = dataSnapshot.getValue<String>()
                if (status == "menu") {
                    statusRef.removeEventListener(statusListener!!)

                    val intent = Intent(this@ResultVC, MenuVC::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    intent.putExtra("roomId", roomId)
                    intent.putExtra("memberId", memberId)
                    intent.putExtra( "uuid", uuid )
                    intent.putExtra("cardCount", cardCount)
                    intent.putExtra("cardColumnCount", cardColumnCount)
                    intent.putExtra("cardRowCount", cardRowCount)
                    startActivity(intent)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }
        statusRef.addValueEventListener(statusListener!!)
    }
}