package com.example.otofuda_android.Menu

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.otofuda_android.Play.PlayVC
import com.example.otofuda_android.R
import com.example.otofuda_android.Response.*
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.marozzi.segmentedtab.SegmentedGroup
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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu)

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

        val API_URL = "https://uniotto.org/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val handler = Handler()

        thread { // Retrofitはメインスレッドで処理できない
            try {
                val service: PresetListService = retrofit.create(PresetListService::class.java)
                val cardCount = resources.getInteger(R.integer.cardColumnCount) * resources.getInteger(
                    R.integer.cardRowCount
                )

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

        val usingMusicSegment = this.findViewById(R.id.usingMusicSegment) as SegmentedGroup
        val scoreModeSegment = this.findViewById(R.id.scoreSegment) as SegmentedGroup
        val playbackModeSegment = this.findViewById(R.id.playbackSegment) as SegmentedGroup

        usingMusicSegment.setOnSegmentedGroupListener { tab, checkedId ->
            var usingMusicModeRef = database.getReference("rooms/" + roomId + "/mode/usingMusic")
            if (tab.text == "プリセット") {
                usingMusicModeRef.setValue("preset")
            } else if (tab.text == "デバイス内") {
                usingMusicModeRef.setValue("device")
            }
        }

        scoreModeSegment.setOnSegmentedGroupListener { tab, checkedId ->
            var scoreModeRef = database.getReference("rooms/" + roomId + "/mode/score")
            if (tab.text == "ノーマル") {
                scoreModeRef.setValue("normal")
            } else if (tab.text == "ビンゴ") {
                scoreModeRef.setValue("bingo")
            }
        }

        playbackModeSegment.setOnSegmentedGroupListener { tab, checkedId ->
            var playbackModeRef = database.getReference("rooms/" + roomId + "/mode/playback")
            if (tab.text == "イントロ") {
                playbackModeRef.setValue("intro")
            } else if (tab.text == "ランダム") {
                playbackModeRef.setValue("random")
            }
        }


    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onStartButtonTapped(view: View?){
        val row1 = presetGroupSpinner!!.selectedItemId.toInt()
        val row2 = presetTitleSpinner!!.selectedItemId.toInt()
        val selectedPreset = presetListArray!![row1].presets[row2]
        val cardCount = resources.getInteger(R.integer.cardColumnCount) * resources.getInteger(R.integer.cardRowCount)
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


    private fun onStatusStarted(){
        val selectedPlayers = myRoom?.selectedPlayers!!
        var playMusicsRef = database.getReference("rooms/" + roomId + "/playMusics")

        selectedPlayers.forEachIndexed{ i, player ->
            if( player == memberId ){
                val playMusic = Music(
                    selectedMusics[i].title,
                    selectedMusics[i].artist,
                    player,
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
            var music = Music(
                playMusic.get("name") as String,
                playMusic.get("artist") as String,
                (playMusic.get("musicOwner") as Long).toInt(),
                playMusic.get("storeURL") as String,
                playMusic.get("previewURL") as String
            )
            playMusics.add(music)
        }

        val intent = Intent(this, PlayVC::class.java)
        intent.putExtra( "uuid", uuid )
        intent.putExtra("roomId", roomId)
        intent.putExtra("playMusics", Gson().toJson(playMusics))
        intent.putExtra("cardLocations", myRoom?.cardLocations?.toIntArray())
        intent.putExtra("memberId", memberId)
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
                val cardCount = resources.getInteger(R.integer.cardColumnCount) * resources.getInteger(
                    R.integer.cardRowCount
                )
                if( myRoom?.playMusics?.size == cardCount && !musicPrepared ){
                    onMusicPrepared()
                    musicPrepared = true
                    roomRef.removeEventListener(this)
                }
                if( myRoom?.mode!!["usingMusic"] == "preset" ){
                    // TODO: Firebaseの値によってSegmentをコントロールしたいけど, そういうファンクションが用意されてなさそうな気が....
                    val usingMusicSegment = this@MenuVC.findViewById(R.id.usingMusicSegment) as SegmentedGroup
//                    usingMusicSegment.
                } else {

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }
        roomRef.addValueEventListener(roomListener)
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
}