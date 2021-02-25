package com.snakamura.otofuda_android.Top

import android.content.Intent

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.snakamura.otofuda_android.R
import java.util.*

class TopVC : AppCompatActivity() {

    val uuid = UUID.randomUUID().toString()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        return true
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onCreateButtonTapped(view: View?){
        val intent = Intent(this, CreateGroupVC::class.java)
        intent.putExtra("uuid", uuid)
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onSearchButtonTapped(view: View?){
        val intent = Intent(this, SearchGroupVC::class.java)
        intent.putExtra("uuid", uuid)
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