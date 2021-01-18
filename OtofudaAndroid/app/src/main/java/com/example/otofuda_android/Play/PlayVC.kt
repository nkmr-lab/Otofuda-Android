package com.example.otofuda_android.Play

import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.RotateAnimation
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.otofuda_android.R
import com.example.otofuda_android.Response.Music
import com.example.otofuda_android.Response.Room
import com.example.otofuda_android.Result.ResultVC
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.play.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class PlayVC : AppCompatActivity() {

    var playMusics = ArrayList<Music>()
    var orderMusics = ArrayList<Music>()
    var currentIndex = 0

    var mediaPlayer: MediaPlayer? = null

    var score = 0

    val database = Firebase.database

    var myRoom: Room? = null

    var uuid = ""

    var roomId = ""

    var memberId = 0

    var customAdapter: CustomAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.play)

        prepareUI()

        val cardCount = resources.getInteger(R.integer.cardColumnCount) * resources.getInteger(R.integer.cardRowCount)

        uuid = intent.getStringExtra("uuid")
        roomId = intent.getStringExtra("roomId")
        memberId = intent.extras.getInt("memberId")

        val cardLocations = intent.getIntArrayExtra("cardLocations")
        val playMusicsStr = intent.extras.getString("playMusics")
        playMusics = Gson().fromJson<ArrayList<Music>>(playMusicsStr, object: TypeToken<ArrayList<Music>>(){}.type)

        for(cardLocation in cardLocations){
            val music = playMusics!![cardLocation]
            orderMusics.add(music)
        }

        // ホスト以外の時
        if( memberId != 0 ){
            val readButton = findViewById<Button>(R.id.readButton)
            readButton.visibility = Button.INVISIBLE
        }

        observeRoomStatus()
        observeRoom()

        customAdapter = CustomAdapter(orderMusics)
        customAdapter!!.setColmunRow(resources.getInteger(R.integer.cardColumnCount), resources.getInteger(R.integer.cardRowCount))
        recycler_view.adapter = customAdapter
        recycler_view.layoutManager = GridLayoutManager(this, resources.getInteger(R.integer.cardColumnCount), RecyclerView.VERTICAL, false)

        customAdapter!!.setOnItemClickListener(object: CustomAdapter.OnItemClickListener {
            override fun onItemClickListener(view: View, position: Int, clickedText: String) {

                // 1970年からの秒数
                val nowTime = Date().time * 60

                println( playMusics!![currentIndex-1].name!! )
                println( orderMusics[position].name!! )

                if( playMusics!![currentIndex-1] == orderMusics[position] ) {


//                    val textView = view.findViewById(R.id.text_view) as TextView
//                    textView.setBackgroundColor(Color.RED)
//                    textView.setTextColor((Color.WHITE))

                    score += 1

                    var statusRef = database.getReference("rooms/" + roomId + "/status")
                    statusRef.setValue("next")

                    // TODO: 効果音鳴らす処理
                    // FIXME: よくみたらanswerタイポしてる.... iOSの方もそうなのでどっちも変える必要あり
                    var answearUserRef = database.getReference("rooms/" + roomId + "/answearUser/" + memberId)
                    answearUserRef.setValue(mapOf("time" to nowTime, "userIndex" to memberId))

                } else {
                    // TODO: 効果音鳴らす処理

                    val otetsukiView =
                        this@PlayVC.findViewById(R.id.otetsukiView) as TextView
                    otetsukiView.visibility = View.VISIBLE

                    Toast.makeText(applicationContext, "正解は、${playMusics!![currentIndex-1].name} でした", Toast.LENGTH_LONG).show()
                }

                // tappedに追加処理
                if( myRoom?.tapped == null ){
                    var tappedDict =  listOf<Map<String, Any>>()
                    var userDict = mapOf("name" to uuid)
                    tappedDict += mapOf( "user" to userDict, "music" to orderMusics[position].name!! )
                    var tappedRef = database.getReference("rooms/" + roomId + "/tapped")
                    tappedRef.setValue(tappedDict)
                } else {
                    var tappedDict = myRoom!!.tapped!!
                    var userDict = mapOf("name" to uuid)
                    tappedDict += mapOf( "user" to userDict, "music" to orderMusics[position].name!! )
                    var tappedRef = database.getReference("rooms/" + roomId + "/tapped")
                    tappedRef.setValue(tappedDict)
                }


                if( currentIndex == cardCount ){
                    finishGame()
                }

                if( memberId == 0 ) {
                    val readButton = this@PlayVC.findViewById(R.id.readButton) as Button
                    readButton.visibility = View.VISIBLE
                }
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
    fun onReadButtonTapped(view: View?){
        var statusRef = database.getReference("rooms/" + roomId + "/status")
        statusRef.setValue("play")
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onBadgeButtonTapped(view: View?){
//        if( currentIndex == 0 ){ return }
//        val playMusic = playMusics!![currentIndex-1]
//        val storeURL = playMusic.storeURL
//        val uri = Uri.parse(storeURL)
//        val intent = Intent(Intent.ACTION_VIEW, uri)
//        startActivity(intent)
        customAdapter!!.setOwner(0)
    }

    private fun prepareUI(){
        supportActionBar?.title = "1曲目"

        val myColorView = this.findViewById(R.id.myColorView) as View

        // TODO: memberIdによって色を変える
        myColorView.setBackgroundColor(Color.RED)
    }

    fun finishGame(){
        if( mediaPlayer != null ){
            mediaPlayer!!.stop()
        }

        val intent = Intent(this, ResultVC::class.java)
        intent.putExtra("playMusics", playMusics)
        intent.putExtra("score", score)
        startActivity(intent)
    }

    fun observeRoom(){
        var roomRef = database.getReference("rooms/" + roomId)

        val roomListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                myRoom = dataSnapshot.getValue<Room>()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }
        roomRef.addValueEventListener(roomListener)
    }

    private fun observeRoomStatus() {
        var roomStatusRef = database.getReference("rooms/" + roomId + "/status")

        val roomStatusListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val status = dataSnapshot.getValue<String>()
                if (status == "play") {
                    onStatusPlayed()
                } else if (status == "menu") {
                    // menuにいく処理
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }
        roomStatusRef.addValueEventListener(roomStatusListener)
    }

    private fun observeAnswearUser(){
        var answearUserRef = database.getReference("rooms/" + roomId + "/answearUser")

        val answearUserListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if( dataSnapshot.childrenCount.toInt() == 0 ){
                    return
                }

                var fastestUser = -1
                var fastestTime = Double.MAX_VALUE

                for( item in dataSnapshot.children ){
                    val snapshot = item!!
                    val dict = snapshot.value as Map<String, Any>

                    val userIndex = dict["userIndex"] as Int
                    val time = dict["time"] as Double

                    if( time < fastestTime ){
                        fastestUser = userIndex
                        fastestTime = time
                    }
                }

                // タップされた札の色をanswearUserにする
                val currentMusic = playMusics[currentIndex - 1]
                currentMusic.musicOwner = fastestUser
//                currentMusic.


            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }
        answearUserRef.addValueEventListener(answearUserListener)
    }

    fun onStatusPlayed(){

        supportActionBar?.title = "${currentIndex+1}曲目"

        if( mediaPlayer != null ){
            mediaPlayer!!.stop()
        }


        val readButton = this.findViewById(R.id.readButton) as Button
        readButton.visibility = View.GONE

        val countDownText = this.findViewById(R.id.countDownText) as TextView
        countDownText.visibility = View.VISIBLE
        countDownText.text = "3"

        val otetsukiView = this.findViewById(R.id.otetsukiView) as TextView
        otetsukiView.visibility = View.GONE


        var count = 3
        val handler = Handler()
        var timer = Timer()
        timer.schedule(1000, 1000) {
            handler.run {
                post {
                    count--
                    countDownText.setText(count.toString())
                    if( count == 0 ){
                        timer.cancel()
                        countDownText.visibility = View.INVISIBLE

                        val playMusic = playMusics!![currentIndex]
                        val musicOwner = playMusics!![currentIndex].musicOwner!!

                        if( musicOwner == memberId ) {
                            val url = playMusic.previewURL
                            mediaPlayer = MediaPlayer().apply {
                                setAudioStreamType(AudioManager.STREAM_MUSIC)
                                setDataSource(url)
                                prepare() // might take long! (for buffering, etc)
                                start()
                            }
                        }

                        currentIndex += 1

                    }
                }
            }
        }
    }
}