package com.example.otofuda_android.Menu

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.otofuda_android.Play.PlayVC
import com.example.otofuda_android.R
import com.example.otofuda_android.Response.Preset
import com.example.otofuda_android.Response.PresetList
import com.example.otofuda_android.Response.PresetListResponse
import com.google.gson.Gson
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import kotlin.concurrent.thread

interface PresetListService {
    @GET("fetch_presets.php?count=16")
    fun fetchPreset(): Call<PresetListResponse>
}

class MenuVC : AppCompatActivity() {

    var presetGroupSpinner: Spinner? = null
    var presetTitleSpinner: Spinner? = null
    var presetListArray: List<PresetList>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.menu)

        supportActionBar?.title = "ルールを選択"

        presetGroupSpinner = this.findViewById(R.id.spinner1) as Spinner
        presetTitleSpinner = this.findViewById(R.id.spinner2) as Spinner

        val API_URL = "https://uniotto.org/api/"
        val retrofit = Retrofit.Builder()
            .baseUrl(API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val handler = Handler()

        thread { // Retrofitはメインスレッドで処理できない
            try {
                val service: PresetListService = retrofit.create(PresetListService::class.java)
                val presetListResponse = service.fetchPreset().execute().body() ?: throw IllegalStateException("bodyがnullだよ！")
                presetListArray = presetListResponse.list

                var presetGroups = ArrayList<String>()
                var presetTitles = ArrayList<ArrayList<String>>()

                handler.post(Runnable {
                    for(presetList in presetListArray!!){
                        val presetTypeName = presetList.type_name
                        val presets = presetList.presets
                        presetGroups.add(presetTypeName)

                        var arrayPresets = ArrayList<String>()

                        for(preset in presets){
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

                    presetGroupSpinner!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                        // 項目が選択された時に呼ばれる
                        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onStartButtonTapped(view: View?){
        val intent = Intent(this, PlayVC::class.java)

        val row1 = presetGroupSpinner!!.selectedItemId.toInt()
        val row2 = presetTitleSpinner!!.selectedItemId.toInt()
        val selectedPreset = presetListArray!![row1].presets[row2]
        val cardLocations = (0..3).toList()
        val shuffledCardLocations = cardLocations.shuffled().toIntArray()

        intent.putExtra("selectedPreset", Gson().toJson(selectedPreset))
        intent.putExtra("cardLocations", shuffledCardLocations)

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