package org.invotek.apps.easyvsd_video_shell

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.support.v4.content.FileProvider



class MainActivity : AppCompatActivity(),
        VideoPlayer.OnFragmentInteractionListener,
        VideoEditor.OnFragmentInteractionListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        if(isStoragePermissionGranted()) {
            loadFragments()
        }
    }

    fun isStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            val permissionsToRequest = ArrayList<String>()
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.CAMERA)
            }
            if (permissionsToRequest.size > 0) {
                var permissionsArray = permissionsToRequest.toTypedArray<String>()
                ActivityCompat.requestPermissions(this@MainActivity, permissionsArray, 1)
                return false
            } else {
                return true
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            return true
        }
    }

    private var videoPlayer : VideoPlayer? = null
    private var videoEditor : VideoEditor? = null
    fun loadFragments(){
        val transaction : FragmentTransaction = supportFragmentManager.beginTransaction()
        var retFrag : Fragment? = null
        if(videoPlayer == null){
            retFrag = supportFragmentManager.findFragmentByTag("videoPlayer")
            if(retFrag != null) {
                videoPlayer = retFrag as VideoPlayer
                if (videoPlayer == null) {
                    videoPlayer = VideoPlayer()
                    videoPlayer?.setMainActivity(this)
                    transaction.add(R.id.activity_main, videoPlayer, "videoPlayer")
                } else {
                    videoPlayer?.setMainActivity(this)
                }
            }else{
                videoPlayer = VideoPlayer()
                videoPlayer?.setMainActivity(this)
                transaction.add(R.id.activity_main, videoPlayer, "videoPlayer")
            }
        }
        if(videoEditor == null){
            retFrag = supportFragmentManager.findFragmentByTag("videoEditor")
            if(retFrag != null) {
                videoEditor = retFrag as VideoEditor
                if (videoEditor == null) {
                    videoEditor = VideoEditor()
                    videoEditor?.setMainActivity(this)
                    transaction.add(R.id.activity_main, videoEditor, "videoEditor")
                } else {
                    videoEditor?.setMainActivity(this)
                }
            }else{
                videoEditor = VideoEditor()
                videoEditor?.setMainActivity(this)
                transaction.add(R.id.activity_main, videoEditor, "videoEditor")
            }
        }
        transaction.commit()
        showVideoPlayer("")
    }

    val cameraVideoPage = 401
    val chooseVideoPage = 411
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        var fileName : String = ""
        when(requestCode){
            cameraVideoPage -> {
                if(data != null) {
                    var extras: Bundle? = data.extras
                    if (extras != null){
                        fileName = extras.get("FILENAME") as String
                        if(TextUtils.isEmpty(fileName)){
                            fileName = data.data.path
                        }
                        showVideoEditor(fileName)
                        return
                    }else{
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                        fileName = prefs.getString("returnedFilename", "")
                        showVideoEditor(fileName)
                        return
                    }
                }else{
                    val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                    fileName = prefs.getString("returnedFilename", "")
                    showVideoEditor(fileName)
                    return
                }
            }
            chooseVideoPage ->{
                fileName = ImageFilePath.getPath(applicationContext, data?.data)
                showVideoEditor(fileName)
                return
            }
            else ->{
                super.onActivityResult(requestCode, resultCode, data)
                showVideoPlayer("")
            }
        }
    }

    private fun showVideoEditor(filename : String){
        if(!TextUtils.isEmpty(filename)){
//            var fileUri = FileProvider.getUriForFile(this@MainActivity,
//            "org.invotek.apps.easyvsd_video_shell.provider",
//            File(filename))
//            if(File(fileUri.path).exists()) {
            if(File(filename).exists()){
                (Thread(Runnable {
                    this@MainActivity.runOnUiThread(java.lang.Runnable {
                        Log.d("MainActivity", "showVideoEditor: [$filename]")
                        var fragmentTransaction: FragmentTransaction = supportFragmentManager.beginTransaction()
                        fragmentTransaction.hide(videoPlayer)
                        fragmentTransaction.show(videoEditor)
                        fragmentTransaction.commit()
                        videoEditor?.setVideoFilename(filename)
                    })
                })).start()
            }else{
                showVideoPlayer("")
            }
        }else{
            showVideoPlayer("")
        }
    }

    fun showVideoPlayer(filename : String){
        (Thread(Runnable{
            this@MainActivity.runOnUiThread(java.lang.Runnable {
                Log.d("MainActivity", "showVideoPlayer: [$filename]")
                var fragmentTransaction : FragmentTransaction = supportFragmentManager.beginTransaction()
                fragmentTransaction.hide(videoEditor)
                fragmentTransaction.show(videoPlayer)
                fragmentTransaction.commit()
                videoPlayer?.setFilename(filename)
            })
        })).start()
    }

    fun getNewVideo(){
        var imageSource = -1
        val chooseBuilder = AlertDialog.Builder(this)
        chooseBuilder.setTitle(R.string.choose_page_image_source)
        var itemsId = R.array.image_sources_video
        chooseBuilder.setSingleChoiceItems(itemsId, -1
        ) { dialog, which ->
            // Save the user's latest choice till they select OK
            imageSource = which
        }
        imageSource = -1        // No source selected
        chooseBuilder.setPositiveButton(R.string.ok, DialogInterface.OnClickListener { dialog, which ->
            when (imageSource) {
                -1    // No source selected
                -> {
                    Toast.makeText(this@MainActivity,
                            "No image source was selected.", Toast.LENGTH_LONG).show()
                    dialog.dismiss()
                }
                0        // Existing Video
                -> {
                    val selectVideoIntent = Intent(Intent.ACTION_PICK,
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
                    startActivityForResult(selectVideoIntent, chooseVideoPage)
                    dialog.dismiss()
                }
                1        // New Video
                -> {
                    val captureVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                    val videoFile = getOutputVideoFile()
                    if (captureVideoIntent.resolveActivity(packageManager) != null) {
//                        val thisUri = FileProvider.getUriForFile(this@MainActivity,
//                                "org.invotek.apps.easyvsd_video_shell.provider",
//                                videoFile)
                        val thisUri = Uri.fromFile(videoFile)
                        captureVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, thisUri)
                        captureVideoIntent.flags += Intent.FLAG_GRANT_READ_URI_PERMISSION
                        val prefs = PreferenceManager.getDefaultSharedPreferences(this@MainActivity)
                        prefs.edit().putString("returnedFilename", thisUri.path).commit()
                        startActivityForResult(captureVideoIntent, cameraVideoPage)
                    }
                    dialog.dismiss()
                }
                else    // Error - should never get here
                -> {
                    Log.i("PlayTalk", "Main.addPage: Error - imageSource=" + Integer.toString(imageSource))
                    dialog.dismiss()
                }
            }
        })
        chooseBuilder.setNegativeButton(R.string.cancel, DialogInterface.OnClickListener { dialog, which ->
            // Nothing to do but close the dialog box
            dialog.dismiss()
        })
        val confirmAlert1 = chooseBuilder.create()
        confirmAlert1.show()
    }

    /** Create a File for saving the video  */
    @SuppressLint("SimpleDateFormat")
    private fun getOutputVideoFile(): File {
        val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), "EasyVSD")
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d("EasyVSD", "failed to create directory")
            }
        }
        //Create a video file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        return File(mediaStorageDir.path + File.separator +
                "VID_" + timeStamp + ".mp4")
    }

    override fun onFragmentInteraction(uri: Uri) {}

}
