package com.example.otofuda_android.Top

import android.content.Intent
import android.graphics.Color
import android.media.MediaPlayer
import android.net.Uri

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.RotateAnimation
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.otofuda_android.R
import com.example.otofuda_android.Response.PresetResponse
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.list_item.view.*
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import kotlin.concurrent.thread

class TopVC : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//
//        val API_URL = "https://uniotto.org/api/"
//        val retrofit = Retrofit.Builder()
//            .baseUrl(API_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//
//        val handler = Handler()
//
//        thread { // Retrofitはメインスレッドで処理できない
//            try {
//                val service:PresetService = retrofit.create(PresetService::class.java)
//                val preset = service.fetchPreset().execute().body() ?: throw IllegalStateException("bodyがnullだよ！")
//                val musics = preset.musics
//
//                handler.post(Runnable {
//
//                    val customAdapter = CustomAdapter(musics)
//                    recycler_view.adapter = customAdapter
//                    recycler_view.layoutManager = GridLayoutManager(this, 4, RecyclerView.VERTICAL, false)
//
//                    customAdapter.setOnItemClickListener(object:CustomAdapter.OnItemClickListener {
//                        override fun onItemClickListener(view: View, position: Int, clickedText: String) {
//
//                            val rotate = RotateAnimation(0.0f, 360.0f*5,
//                                Animation.RELATIVE_TO_SELF, 0.5f,
//                                Animation.RELATIVE_TO_SELF, 0.5f);
//
//                            // animation時間 msec
//                            rotate.setDuration(2000);
//                            // animationが終わったそのまま表示にする
//                            rotate.setFillAfter(true);
//
//                            view.startAnimation(rotate)
//
//                            val textView = view.findViewById(R.id.text_view) as TextView
//                            textView.setBackgroundColor(Color.RED)
//                            textView.setTextColor((Color.WHITE))
//                            Toast.makeText(applicationContext, "${clickedText}がタップされました", Toast.LENGTH_LONG).show()
//                        }
//                    })
//                })
//
//            } catch (e: Exception) {
//                println("debug $e")
//            }
//        }
//

//        val button = this.findViewById(R.id.button) as Button
//        button.setOnClickListener(View.OnClickListener {
//            val storeURL =
//                "https://music.apple.com/jp/album/%E5%A4%A2%E8%BF%BD%E3%81%84%E3%83%99%E3%83%B3%E3%82%AC%E3%83%AB/1446781785?i=1446781793&uo=4"
//            val uri = Uri.parse(storeURL)
//            val intent = Intent(Intent.ACTION_VIEW, uri)
//            startActivity(intent)
//        })
//

//        val url = "https://audio-ssl.itunes.apple.com/itunes-assets/AudioPreview113/v4/0f/ad/7e/0fad7e2f-2d98-4d02-8f00-b3911661fc05/mzaf_16869530692142746138.plus.aac.p.m4a" // your URL here
//        val mediaPlayer: MediaPlayer? = MediaPlayer().apply {
//            setAudioStreamType(AudioManager.STREAM_MUSIC)
//            setDataSource(url)
//            prepare() // might take long! (for buffering, etc)
//            start()
//        }


        val database = Firebase.database
        val myRef = database.getReference("rooms")

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onButtonTapped(view: View?){
        val intent = Intent(this, CreateGroupVC::class.java)
        startActivity(intent)
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