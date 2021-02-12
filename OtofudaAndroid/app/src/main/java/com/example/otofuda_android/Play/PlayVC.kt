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
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.otofuda_android.R
import com.example.otofuda_android.Response.Music
import com.example.otofuda_android.Response.Room
import com.example.otofuda_android.Result.ResultVC
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.play.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import android.media.AudioAttributes
import android.media.SoundPool
import com.google.firebase.database.*

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

    var memberCount = 0

    var myColor: Int = Color.RED

    var customAdapter: CustomAdapter? = null

    // FIXME: 変数名の変更
    /* LocationというよりOrderとかIndexesのほうがいいかも */
    var cardLocations = intArrayOf()

    var playMusicLocations = arrayOf<Int>()

    lateinit var soundPool: SoundPool

    var soundOne = 0
    var soundTwo = 1

    var isTapped = false
    var isPlaying = false

    var roomListener: ValueEventListener? = null
    var statusListener: ValueEventListener? = null
    var answearUserListener: ValueEventListener? = null
    var allOtetsukiListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.play)

        val cardCount = resources.getInteger(R.integer.cardColumnCount) * resources.getInteger(R.integer.cardRowCount)

        uuid = intent.getStringExtra("uuid")
        roomId = intent.getStringExtra("roomId")
        memberId = intent.extras.getInt("memberId")
        memberCount = intent.extras.getInt("memberCount")

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            // ストリーム数に応じて
            .setMaxStreams(2)
            .build()

        // タップした時の効果音 をロードしておく
        soundOne = soundPool.load(this, R.raw.tap_fuda, 1)
        soundTwo = soundPool.load(this, R.raw.otetsuki_voice, 1)

        val colors = resources.obtainTypedArray(R.array.userColors)
        myColor = colors.getColor(memberId, 0)

        // 自分の色などをセット
        prepareUI()

        val playMusicsStr = intent.extras.getString("playMusics")
        playMusics = Gson().fromJson<ArrayList<Music>>(playMusicsStr, object: TypeToken<ArrayList<Music>>(){}.type)

        println("====== 流れる予定の曲 ========")
        for( (index, playMusic) in playMusics.withIndex() ){
            println( "$index, " + playMusic.name )
        }
        println("================")

        cardLocations = intent.getIntArrayExtra("cardLocations")

        for(cardLocation in cardLocations){
            val music = playMusics!![cardLocation]
            orderMusics.add(music)
        }

        for(i in 0 until cardCount){
            val playMusicLocation = cardLocations.indexOf(i)
            playMusicLocations += playMusicLocation
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

        observeTimestamp()

        customAdapter!!.setOnItemClickListener(object: CustomAdapter.OnItemClickListener {
            override fun onItemClickListener(view: View, position: Int, clickedText: String) {

                val date = ServerValue.TIMESTAMP
                var timestampRef = database.getReference("rooms/" + roomId + "/timestamp")
                timestampRef.setValue(date)

                // 再生中じゃなかったりタップ済だったら何もしない
                if( !isPlaying ){ return }
                if( isTapped ){ return }

                isTapped = true
                isPlaying = false

                if( playMusics!![currentIndex-1] == orderMusics[position] ) {

                    // FIXME: よくみたらanswerタイポしてる.... iOSの方もそうなのでどっちも変える必要あり

                    var answearUserRef = database.getReference("rooms/" + roomId + "/answearUser/" + memberId)
                    answearUserRef.setValue(mapOf("time" to ServerValue.TIMESTAMP, "userIndex" to memberId))

                } else {

                    // play(ロードしたID, 左音量, 右音量, 優先度, ループ, 再生速度)
                    soundPool.play(soundTwo, 1.0f, 1.0f, 0, 0, 1.0f)

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
            }
        })
        
        observeAnswearUser()
        observeAllOtetsuki()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onReadButtonTapped(view: View?){
        var statusRef = database.getReference("rooms/" + roomId + "/status")
        statusRef.setValue("play")

        var tappedRef = database.getReference("rooms/" + roomId + "/tapped")
        tappedRef.removeValue()

        var answearUserRef = database.getReference("rooms/" + roomId + "/answearUser")
        answearUserRef.removeValue()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onBadgeButtonTapped(view: View?){
        if( currentIndex == 0 ){ return }
        val playMusic = playMusics!![currentIndex-1]
        val storeURL = playMusic.storeURL
        val uri = Uri.parse(storeURL)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    private fun prepareUI(){
        supportActionBar?.title = "1曲目"

        val myColorView = this.findViewById(R.id.myColorView) as View

        // TODO: memberIdによって色を変える
        myColorView.setBackgroundColor(myColor)
    }

    fun finishGame(){
        if( mediaPlayer != null ){
            mediaPlayer!!.stop()
        }

        var roomRef = database.getReference("rooms/" + roomId)

        if( memberId == 0 ) {
            roomRef.child("answearUser").removeValue()
            roomRef.child("cardLocations").removeValue()
            roomRef.child("playMusics").removeValue()
            roomRef.child("selectedPlayers").removeValue()
            roomRef.child("tapped").removeValue()
        }

        roomRef.removeEventListener(roomListener!!)

        var statusRef = database.getReference("rooms/" + roomId + "/status")
        statusRef.removeEventListener(statusListener!!)

        var answearUserRef = database.getReference("rooms/" + roomId + "/answearUser")
        answearUserRef.removeEventListener(answearUserListener!!)

        var tappedRef = database.getReference("rooms/" + roomId + "/tapped")
        tappedRef.removeEventListener(allOtetsukiListener!!)

        val intent = Intent(this, ResultVC::class.java)
        intent.putExtra("playMusics", playMusics)
        intent.putExtra("score", score)
        intent.putExtra("roomId", roomId)
        intent.putExtra("memberId", memberId)
        intent.putExtra( "uuid", uuid )
        startActivity(intent)
    }

    fun observeRoom(){
        var roomRef = database.getReference("rooms/" + roomId)

        roomListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                myRoom = dataSnapshot.getValue<Room>()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }
        roomRef.addValueEventListener(roomListener!!)
    }

    fun observeTimestamp(){
        var timestampRef = database.getReference("rooms/" + roomId + "/timestamp")
        var isFirst = true

        var timestampListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val timestamp = dataSnapshot.getValue<Long>()
                timestamp?.let {
                    if( !isFirst ) {
                        println("  timestamp:${timestamp}")
                    }
                    isFirst = false
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }

        timestampRef.addValueEventListener(timestampListener!!)
    }

    private fun observeRoomStatus() {
        var statusRef = database.getReference("rooms/" + roomId + "/status")

        statusListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val status = dataSnapshot.getValue<String>()
                if (status == "play") {
                    onStatusPlayed()
                } else if (status == "menu") {
                    // menuにいく処理
                } else if ( status == "next" ){
                    if( memberId == 0 ) {
                        val readButton = this@PlayVC.findViewById(R.id.readButton) as Button
                        readButton.visibility = View.VISIBLE
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }
        statusRef.addValueEventListener(statusListener!!)
    }


    /* 誰かが正解したらこの関数が呼ばれる */
    private fun observeAnswearUser(){
        var answearUserRef = database.getReference("rooms/" + roomId + "/answearUser")

        answearUserListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if( dataSnapshot.childrenCount.toInt() == 0 ){
                    return
                }

                println( "=====================" )
                println( dataSnapshot.value )
                println( "=====================" )

                var fastestUser = -1
                var fastestTime = Long.MAX_VALUE

                for( item in dataSnapshot.children ){
                    val snapshot = item!!
                    val dict = snapshot.value as Map<String, Any>

                    val userIndex = dict["userIndex"] as Long
                    val time = dict["time"] as Long

                    if( time < fastestTime ){
                        fastestUser = userIndex.toInt()
                        fastestTime = time
                    }
                }

                // タップされた札の色をanswearUserにする
                val currentMusic = playMusics[currentIndex - 1]
                currentMusic.musicOwner = fastestUser

                val playMusicLocation = playMusicLocations[currentIndex-1]

                val colors = resources.obtainTypedArray(R.array.userColors)
                val color = colors.getColor(fastestUser, 0)

                customAdapter!!.setOwner(playMusicLocation, color)

                // 自分だったら得点を追加する
                if( fastestUser == memberId ){
                    score += 1

                    // play(ロードしたID, 左音量, 右音量, 優先度, ループ, 再生速度)
                    soundPool.play(soundOne, 1.0f, 1.0f, 0, 0, 1.0f)
                }

                val cardCount = resources.getInteger(R.integer.cardColumnCount) * resources.getInteger(R.integer.cardRowCount)
                if( currentIndex == cardCount ){
                    finishGame()
                    return
                }

                isPlaying = false
                isTapped = false

                // 次の曲に進む
                var statusRef = database.getReference("rooms/" + roomId + "/status")
                statusRef.setValue("next")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }
        answearUserRef.addValueEventListener(answearUserListener!!)
    }

    /* 全員がお手つきした時はこの関数が呼ばれる */
    fun observeAllOtetsuki(){

        var tappedRef = database.getReference("rooms/" + roomId + "/tapped")

        allOtetsukiListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if( dataSnapshot.childrenCount.toInt() != memberCount ){
                    return
                }

                val cardCount = resources.getInteger(R.integer.cardColumnCount) * resources.getInteger(R.integer.cardRowCount)
                if( currentIndex == cardCount ){
                    finishGame()
                    return
                }

                isPlaying = false
                isTapped = false

                // 次の曲に進む
                var statusRef = database.getReference("rooms/" + roomId + "/status")
                statusRef.setValue("next")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }
        tappedRef.addValueEventListener(allOtetsukiListener!!)
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

                        isPlaying = true

                        val playMusic = playMusics!![currentIndex]
                        val musicOwner = playMusics!![currentIndex].musicOwner!!

                        if( musicOwner == memberId ) {
                            val url = playMusic.previewURL
                            mediaPlayer = MediaPlayer().apply {
                                setAudioStreamType(AudioManager.STREAM_MUSIC)
                                setDataSource(url)
                                isLooping = true
                                prepare()
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