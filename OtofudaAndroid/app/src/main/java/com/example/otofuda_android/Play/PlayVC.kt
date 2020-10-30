package com.example.otofuda_android.Play

import android.content.Intent
import android.graphics.Color
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
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
import com.example.otofuda_android.Response.Preset
import com.example.otofuda_android.Response.PresetResponse
import com.example.otofuda_android.Result.ResultVC
import com.google.gson.Gson
import kotlinx.android.synthetic.main.play.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule
import kotlin.concurrent.thread

interface PresetService {
    @GET("get_preset.php")
    fun get(@Query("id") id: Int, @Query("count") count: Int): Call<PresetResponse>
}

class PlayVC : AppCompatActivity() {

    var playMusics = ArrayList<Music>()
    var orderMusics = ArrayList<Music>()
    var currentIndex = 0

    var mediaPlayer: MediaPlayer? = null

    var score = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.play)

        prepareUI()

        val selectedPresetJson = intent.extras.getString("selectedPreset")
        val selectedPreset = Gson().fromJson<Preset>(selectedPresetJson, Preset::class.java)

        val cardLocations = intent.extras.getIntArray("cardLocations")!!

        val API_URL = "https://uniotto.org/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val handler = Handler()

        thread { // Retrofitはメインスレッドで処理できない
            try {
                val service: PresetService = retrofit.create(PresetService::class.java)
                val preset = service.get(selectedPreset.id, 4).execute().body() ?: throw IllegalStateException("bodyがnullだよ！")
                playMusics = preset.musics

                println(playMusics)
                for(cardLocation in cardLocations){
                    val music = playMusics!![cardLocation]
                    orderMusics.add(music)
                }

                handler.post(Runnable {
                    for(music in orderMusics){
                        print(music)
                    }
                    val customAdapter = CustomAdapter(orderMusics)
                    recycler_view.adapter = customAdapter
                    recycler_view.layoutManager = GridLayoutManager(this, 4, RecyclerView.VERTICAL, false)

                    customAdapter.setOnItemClickListener(object: CustomAdapter.OnItemClickListener {
                        override fun onItemClickListener(view: View, position: Int, clickedText: String) {

                            println( playMusics!![currentIndex-1].title )
                            println( orderMusics[position].title)

                            if( playMusics!![currentIndex-1] == orderMusics[position] ) {


                                val rotate = RotateAnimation(
                                    0.0f, 360.0f * 5,
                                    Animation.RELATIVE_TO_SELF, 0.5f,
                                    Animation.RELATIVE_TO_SELF, 0.5f
                                );

                                // animation時間 msec
                                rotate.setDuration(2000);
                                // animationが終わったそのまま表示にする
                                rotate.setFillAfter(true);

                                view.startAnimation(rotate)

                                val textView = view.findViewById(R.id.text_view) as TextView
                                textView.setBackgroundColor(Color.RED)
                                textView.setTextColor((Color.WHITE))

                                score += 1

                            } else {
                                val otetsukiView =
                                    this@PlayVC.findViewById(R.id.otetsukiView) as TextView
                                otetsukiView.visibility = View.VISIBLE

                                Toast.makeText(applicationContext, "正解は、${playMusics!![currentIndex-1].title} でした", Toast.LENGTH_LONG).show()
                            }

                            if( currentIndex == 4 ){
                                finishGame()
                            }

                            val readButton = this@PlayVC.findViewById(R.id.readButton) as Button
                            readButton.visibility = View.VISIBLE
                        }
                    })
                })

            } catch (e: Exception) {
                println("debug $e")
            }
        }
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

                        val url = playMusic.preview_url
                        mediaPlayer = MediaPlayer().apply {
                            setAudioStreamType(AudioManager.STREAM_MUSIC)
                            setDataSource(url)
                            prepare() // might take long! (for buffering, etc)
                            start()
                            currentIndex += 1
                        }
                    }
                }
            }
        }

    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onBadgeButtonTapped(view: View?){
        if( currentIndex == 0 ){ return }
        val playMusic = playMusics!![currentIndex-1]
        val storeURL = playMusic.store_url
        val uri = Uri.parse(storeURL)
        val intent = Intent(Intent.ACTION_VIEW, uri)
        startActivity(intent)
    }

    fun prepareUI(){
        supportActionBar?.title = "1曲目"

        val myColorView = this.findViewById(R.id.myColorView) as View
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
}