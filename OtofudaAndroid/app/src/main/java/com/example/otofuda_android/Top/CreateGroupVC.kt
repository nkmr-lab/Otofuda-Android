
package com.example.otofuda_android.Top

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.otofuda_android.R
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.example.otofuda_android.Extensions.*
import com.example.otofuda_android.Menu.MenuVC
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.create_group.*
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class CreateGroupVC : AppCompatActivity() {

    val database = Firebase.database
    var memberRef: DatabaseReference? = null
    var memberListener: ValueEventListener? = null

    var roomId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.create_group)
        supportActionBar?.title = "グループを作成"

        roomId = createGroup()
        observeMember()

        println("===============")
        println(roomId)
        println("===============")

        var roomUrl = "https://uniotto.org/api/searchRoom.php?roomID=${roomId}"

        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(roomUrl, BarcodeFormat.QR_CODE, 1500, 1500)
            val imageQr = findViewById<ImageView>(R.id.imageView)
            imageQr.setImageBitmap(bitmap)
        } catch (e: Exception) {

        }

    }

    private fun createGroup(): String {
        val roomId = getRandomStringWithLength(6)
        val roomRef = database.getReference("rooms/" + roomId)
        val uuid = UUID.randomUUID().toString()
        var member = listOf<String>(uuid)
        val mode = mapOf("playback" to "intro", "score" to "normal", "usingMusic" to "preset")
        val status = "menu"
        val current = Date()
        val format = SimpleDateFormat("yyyy-MM-dd-HH-mm", Locale.getDefault())
        val date = format.format(current)
        val roomDict = mapOf( "date" to date, "name" to roomId, "member" to member, "mode" to mode, "status" to status)
        roomRef.setValue(roomDict)
        return roomId
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun onCreateButtonTapped(view: View?){
        val intent = Intent(this, MenuVC::class.java)
        startActivity(intent)
        // メンバーのイベントリスナーを削除
        memberRef!!.removeEventListener(memberListener!!)
    }

    fun observeMember(){
        memberRef = database.getReference("rooms/" + roomId + "/member")
        memberListener = object: ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val member = dataSnapshot.value as List<String>
                var memberCountLabel =
                    this@CreateGroupVC.findViewById(R.id.memberCountLabel) as TextView
                memberCountLabel.text = member.size.toString()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }

        memberRef!!.addValueEventListener(memberListener!!)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // ルーム情報とメンバーのイベントリスナーを削除
        val roomsRef = database.getReference("rooms/" + roomId)
        roomsRef.removeValue()
        memberRef!!.removeEventListener(memberListener!!)
    }



}
