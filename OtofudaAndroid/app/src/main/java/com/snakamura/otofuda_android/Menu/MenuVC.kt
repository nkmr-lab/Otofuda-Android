package com.snakamura.otofuda_android.Menu

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.snakamura.otofuda_android.Play.PlayVC
import com.snakamura.otofuda_android.R
import com.snakamura.otofuda_android.Response.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.marozzi.segmentedtab.SegmentedGroup
import com.marozzi.segmentedtab.SegmentedTab
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import kotlin.concurrent.thread


interface PresetListService {
    @GET("fetch_presets.php")
    fun fetchPreset(@Query("count") count: Int): Call<PresetListResponse>
}

interface PresetService {
    @GET("get_preset.php")
    fun get(@Query("id") id: Int, @Query("count") count: Int): Call<PresetResponse>
}

class MenuVC : AppCompatActivity() {

    var presetGroupSpinner: Spinner? = null
    var presetTitleSpinner: Spinner? = null
    var presetListArray: List<PresetList>? = null

    val database = Firebase.database

    var uuid: String = ""

    var roomId = ""

    var memberId = 0

    var myRoom: Room? = null

    var playMusics = ArrayList<MusicResponse>()
    var selectedMusics = ArrayList<MusicResponse>()

    var nextButton: Button? = null

    var cardCount = 0
    var cardColumnCount = 0
    var cardRowCount = 0

    var usingMusicSegment: SegmentedGroup? = null
    var scoreModeSegment: SegmentedGroup? = null
    var playbackModeSegment: SegmentedGroup? = null
    var cardCountSegment: SegmentedGroup?= null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu)

        val putCardCount = intent.extras.getInt("cardCount", -1)
        println("========")
        println(putCardCount)
        if( putCardCount == -1 ) {
            cardColumnCount = resources.getInteger(R.integer.cardColumnCount)
            cardRowCount = resources.getInteger(R.integer.cardRowCount)
            cardCount = cardColumnCount * cardRowCount

        } else {
            cardColumnCount = intent.extras.getInt("cardColumnCount")
            cardRowCount = intent.extras.getInt("cardRowCount")
            cardCount = intent.extras.getInt("cardCount")
        }

        // プリセットが読み込まれるまではボタンを押せないようにする
        nextButton = this.findViewById(R.id.nextButton) as Button
        nextButton?.isClickable = false

        supportActionBar?.title = "ルールを選択"

        uuid =intent.extras.getString("uuid")
        roomId = intent.extras.getString("roomId")
        memberId = intent.extras.getInt("memberId")

        observeRoom()

        presetGroupSpinner = this.findViewById(R.id.spinner1) as Spinner
        presetTitleSpinner = this.findViewById(R.id.spinner2) as Spinner

        val clientView = this.findViewById(R.id.clientView) as FrameLayout

        // メンバーだったらメニューを操作不可能にする
        if( memberId == 0 ) {
            clientView.visibility = View.GONE
        } else {
            clientView.visibility = View.VISIBLE
            clientView.setOnClickListener(null)
        }

        usingMusicSegment = this.findViewById(R.id.usingMusicSegment) as SegmentedGroup
        scoreModeSegment = this.findViewById(R.id.scoreSegment) as SegmentedGroup
        playbackModeSegment = this.findViewById(R.id.playbackSegment) as SegmentedGroup
        cardCountSegment = this.findViewById(R.id.cardCountSegment) as SegmentedGroup

        // TODO: cardCountの値によってcardCountSegmentを変えたい
        when (cardCount) {
            2*2 -> {
                val selectedTab = this.findViewById(R.id.cardCountSegmentTab2x2) as SegmentedTab
                cardCountSegment?.selected(selectedTab.id)
            }
            3*3 -> {
                val selectedTab = this.findViewById(R.id.cardCountSegmentTab3x3) as SegmentedTab
                cardCountSegment?.selected(selectedTab.id)
            }
            else -> {
                val selectedTab = this.findViewById(R.id.cardCountSegmentTab4x4) as SegmentedTab
                cardCountSegment?.selected(selectedTab.id)
            }
        }

        usingMusicSegment?.setOnSegmentedGroupListener { tab, checkedId ->
            var usingMusicModeRef = database.getReference("rooms/" + roomId + "/mode/usingMusic")
            if (tab.text == "プリセット") {
                usingMusicModeRef.setValue("preset")
            } else if (tab.text == "デバイス内") {
                usingMusicModeRef.setValue("device")
            }
        }

        scoreModeSegment?.setOnSegmentedGroupListener { tab, checkedId ->
            var scoreModeRef = database.getReference("rooms/" + roomId + "/mode/score")
            if (tab.text == "ノーマル") {
                scoreModeRef.setValue("normal")
            } else if (tab.text == "ビンゴ") {
                scoreModeRef.setValue("bingo")
            }
        }

        playbackModeSegment?.setOnSegmentedGroupListener { tab, checkedId ->
            var playbackModeRef = database.getReference("rooms/" + roomId + "/mode/playback")
            if (tab.text == "イントロ") {
                playbackModeRef.setValue("intro")
            } else if (tab.text == "ランダム") {
                playbackModeRef.setValue("random")
            }
        }

        cardCountSegment?.setOnSegmentedGroupListener { tab, checkedId ->
            var cardCountModeRef = database.getReference("rooms/" + roomId + "/mode/cardCount")
            when (tab.text) {
                "2x2" -> {
                    cardCountModeRef.setValue("2x2")
                    cardColumnCount = 2
                    cardRowCount = 2
                    cardCount = cardColumnCount * cardRowCount
                }
                "3x3" -> {
                    cardCountModeRef.setValue("3x3")
                    cardColumnCount = 3
                    cardRowCount = 3
                    cardCount = cardColumnCount * cardRowCount
                }
                "4x4" -> {
                    cardCountModeRef.setValue("4x4")
                    cardColumnCount = 4
                    cardRowCount = 4
                    cardCount = cardColumnCount * cardRowCount
                }
            }

            preparePresets()
        }

        preparePresets()
    }


    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onStartButtonTapped(view: View?){
        val row1 = presetGroupSpinner!!.selectedItemId.toInt()
        val row2 = presetTitleSpinner!!.selectedItemId.toInt()
        val selectedPreset = presetListArray!![row1].presets[row2]
        val cardLocations = (0..cardCount-1).toList()
        val shuffledCardLocations = cardLocations.shuffled()
        var shuffledPlayers = List(cardCount){0}
        var roomRef = database.getReference("rooms/" + roomId)

        roomRef.child("selectedPlayers").setValue(shuffledPlayers)
        roomRef.child("cardLocations").setValue(shuffledCardLocations)

        // todo: shuffleplayerを指定する処理を書く
        if( true ){
            // todo: デバイス内の曲モードだったらここでシャッフル処理書く
        }

        val API_URL = "https://uniotto.org/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        thread { // Retrofitはメインスレッドで処理できない
            try {
                val service: PresetService = retrofit.create(PresetService::class.java)
                val preset = service.get(selectedPreset.id, cardCount).execute().body() ?: throw IllegalStateException(
                    "bodyがnullだよ！"
                )
                playMusics = preset.musics

                println(playMusics)
                for(cardLocation in cardLocations) {
                    val music = playMusics!![cardLocation]
                    selectedMusics.add(music)
                }

                roomRef.child("status").setValue("start")
            }
            catch (e: Exception) {
                println("debug $e")
            }
        }
    }

    private fun preparePresets(){
        val API_URL = "https://uniotto.org/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val handler = Handler()

        thread { // Retrofitはメインスレッドで処理できない
            try {
                val service: PresetListService = retrofit.create(PresetListService::class.java)

                val presetListResponse =
                    service.fetchPreset(cardCount).execute().body() ?: throw IllegalStateException(
                        "bodyがnullだよ！"
                    )
                presetListArray = presetListResponse.list

                var presetGroups = ArrayList<String>()
                var presetTitles = ArrayList<ArrayList<String>>()

                handler.post(Runnable {
                    for (presetList in presetListArray!!) {
                        val presetTypeName = presetList.type_name
                        val presets = presetList.presets
                        presetGroups.add(presetTypeName)

                        var arrayPresets = ArrayList<String>()

                        for (preset in presets) {
                            arrayPresets.add(preset.name)
                        }

                        presetTitles.add(arrayPresets)
                    }

                    val presetGroupAdapter = ArrayAdapter<String>(
                        this,
                        R.layout.custom_spinner,
                        presetGroups
                    )

                    val presetTitleAdapter = ArrayAdapter<String>(
                        this,
                        R.layout.custom_spinner,
                        presetTitles[0]
                    )

                    presetGroupAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown)
                    presetGroupSpinner!!.setAdapter(presetGroupAdapter)

                    // FIX: なぜかドロップダウン中にも, ▼ が表示されてしまっている
                    presetTitleAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown)
                    presetTitleSpinner!!.setAdapter(presetTitleAdapter)

                    nextButton?.isClickable = true

                    presetGroupSpinner!!.onItemSelectedListener =
                        object : AdapterView.OnItemSelectedListener {

                            // 項目が選択された時に呼ばれる
                            override fun onItemSelected(
                                parent: AdapterView<*>?,
                                view: View?,
                                position: Int,
                                id: Long
                            ) {
                                val selectedPresetTitleAdapter = ArrayAdapter<String>(
                                    this@MenuVC,
                                    R.layout.custom_spinner,
                                    presetTitles[position]
                                )

                                presetTitleAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown)
                                presetTitleSpinner!!.setAdapter(selectedPresetTitleAdapter)
                            }

                            // 基本的には呼ばれないが、何らかの理由で選択されることなく項目が閉じられたら呼ばれる
                            override fun onNothingSelected(parent: AdapterView<*>?) {

                            }
                        }
                })
            } catch (e: Exception) {
                println("debug $e")
            }
        }
    }


    private fun onStatusStarted(){

        // カードの枚数をFirebaseから読み込む( ホストは読み込む必要ないが )
        loadCardCount()

        val selectedPlayers = myRoom?.selectedPlayers!!
        var playMusicsRef = database.getReference("rooms/" + roomId + "/playMusics")

        selectedPlayers.forEachIndexed { i, player ->
            if (player == memberId) {
                val playMusic = Music(
                    selectedMusics[i].title,
                    selectedMusics[i].artist,
                    player,
                    -1,
                    selectedMusics[i].store_url,
                    selectedMusics[i].preview_url
                )
                playMusicsRef.child(i.toString()).setValue(playMusic)
            }
        }
    }

    private fun onMusicPrepared(){

        var playMusics = arrayListOf<Music>()
        for(playMusic in myRoom?.playMusics!!){
            playMusics.add(playMusic)
        }

        val intent = Intent(this, PlayVC::class.java)
        intent.putExtra( "uuid", uuid )
        intent.putExtra("roomId", roomId)
        intent.putExtra("playMusics", Gson().toJson(playMusics))
        intent.putExtra("cardLocations", myRoom?.cardLocations?.toIntArray())
        intent.putExtra("memberId", memberId)
        intent.putExtra("memberCount", myRoom?.member?.size)
        intent.putExtra("cardCount", cardCount)
        intent.putExtra("cardColumnCount", cardColumnCount)
        intent.putExtra("cardRowCount", cardRowCount)
        startActivity(intent)
    }

    private fun observeRoom() {
        var roomRef = database.getReference("rooms/" + roomId)
        var statusStarted = false
        var musicPrepared = false

        val roomListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                myRoom = dataSnapshot.getValue<Room>()
                if( myRoom?.status == "start" && !statusStarted ) {
                    onStatusStarted()
                    statusStarted = true
                }
                if( myRoom?.playMusics?.size == cardCount && !musicPrepared ){
                    onMusicPrepared()
                    musicPrepared = true
                    roomRef.removeEventListener(this)
                }
                if( myRoom?.mode!!["usingMusic"] == "preset" ){
                    // TODO: Firebaseの値によってSegmentをコントロールしたいけど, そういうファンクションが用意されてなさそうな気が....
                    val selectedTab = this@MenuVC.findViewById(R.id.usingMusicSegmentTabPreset) as SegmentedTab
                    usingMusicSegment?.selected(selectedTab.id)
                } else {
                    val selectedTab = this@MenuVC.findViewById(R.id.usingMusicSegmentTabDevice) as SegmentedTab
                    usingMusicSegment?.selected(selectedTab.id)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }
        roomRef.addValueEventListener(roomListener)
    }

    @Override
    override fun onBackPressed() {}

    private fun loadCardCount(){
        val cardCountMode = myRoom?.mode?.get("cardCount")

        if( cardCountMode == "2x2" ){
            cardColumnCount = 2
            cardRowCount = 2
            cardCount = cardColumnCount * cardRowCount
//            val selectedTab = this.findViewById(R.id.cardCountSegmentTab2x2) as SegmentedTab
//            cardCountSegment?.selected(selectedTab.id)
        } else if( cardCountMode == "3x3" ){
            cardColumnCount = 3
            cardRowCount = 3
            cardCount = cardColumnCount * cardRowCount
//            val selectedTab = this.findViewById(R.id.cardCountSegmentTab3x3) as SegmentedTab
//            cardCountSegment?.selected(selectedTab.id)
        } else if( cardCountMode == "4x4" ){
            cardColumnCount = 4
            cardRowCount = 4
            cardCount = cardColumnCount * cardRowCount
//            val selectedTab = this.findViewById(R.id.cardCountSegmentTab4x4) as SegmentedTab
//            cardCountSegment?.selected(selectedTab.id)
        }
    }
}