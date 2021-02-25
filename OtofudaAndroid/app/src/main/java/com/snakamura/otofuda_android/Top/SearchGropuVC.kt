package com.snakamura.otofuda_android.Top

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.snakamura.otofuda_android.Menu.MenuVC
import com.snakamura.otofuda_android.R
import com.snakamura.otofuda_android.Response.Room
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView


class SearchGroupVC : AppCompatActivity() {

    val database = Firebase.database

    var qr_view: CompoundBarcodeView? = null

    var rooms: Map<String, Room>? = null

    var uuid: String = ""

    var roomsListener: ValueEventListener? = null

    companion object {
        const val REQUEST_CAMERA_PERMISSION:Int = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.search_group)
        supportActionBar?.title = "グループを検索"
        uuid =intent.extras.getString("uuid")

        qr_view = this.findViewById(R.id.qr_view) as CompoundBarcodeView
        observeRooms()
        checkPermissions()
        initQRCamera()
    }


    private fun checkPermissions() {
        // already we got permission.
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            qr_view?.resume()
        }

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.CAMERA)) {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 999)
        }
    }

    @SuppressLint("WrongConstant")
    private fun initQRCamera() {
        println("カメラ初期化しやしたあああああ")

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        val isCameraPermissionGranted = (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)

        if (isCameraPermissionGranted) {
            openQRCamera() // ← カメラ起動
            println("カメラ起動しやしたあああああああ")
        } else {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
        }
    }

    private fun openQRCamera(){
        qr_view?.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                if (result != null) {
                    onPause()
                    Log.d("QRCode", "$result")
                    val resultStr = result.toString()
                    var separatedURL = resultStr.split("=")
                    if (separatedURL.size > 1) {
                        val roomId = separatedURL[1]
                        println(roomId)
                        for ((roomName, room) in rooms!!) {
                            if (roomName == roomId) {
                                qr_view?.pause()
                                val memberRef = database.getReference("rooms/" + roomId + "/" + "member")
                                var member = rooms!![roomId]!!.member!!.toMutableList()
                                member.add(uuid)
                                memberRef.setValue(member.toList())

                                var roomsRef = database.getReference("rooms/")
                                roomsRef.removeEventListener(roomsListener!!)

                                val intent = Intent(this@SearchGroupVC, MenuVC::class.java)
                                intent.putExtra("roomId", roomId)
                                intent.putExtra("memberId", member.size-1)
                                intent.putExtra( "uuid", uuid )
                                startActivity(intent)
                            }
                        }
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                checkPermissions()
                initQRCamera()
            }
        }
    }

    private fun observeRooms() {
        var roomsRef = database.getReference("rooms/")

        roomsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                rooms = dataSnapshot.getValue<Map<String, Room>>()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("onCancelled", "error:", error.toException())
            }
        }
        roomsRef.addValueEventListener(roomsListener!!)
    }

}