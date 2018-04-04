package org.invotek.apps.easyvsd_video_shell

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import com.waynell.videorangeslider.RangeSlider


import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.support.v4.app.Fragment
import android.util.Log
import android.view.*
import android.widget.*
import com.googlecode.mp4parser.FileDataSourceViaHeapImpl
import com.googlecode.mp4parser.authoring.Track
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator
import com.googlecode.mp4parser.authoring.tracks.AppendTrack
import com.googlecode.mp4parser.authoring.tracks.CroppedTrack

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.*

import org.invotek.apps.interfaces.OnTrimVideoListener
/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [VideoEditor.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [VideoEditor.newInstance] factory method to
 * create an instance of this fragment.
 */
class VideoEditor : Fragment(), OnTrimVideoListener {


    private var mListener: OnFragmentInteractionListener? = null
    private var videoView: VideoView? = null
    private var rangeSeekBar: RangeSlider? = null
    private val TAG = "VideoEditor"
    private var playView : ImageView? = null
    private var duration: Int? = null
    private var tvLeft: TextView? = null
    private var tvRight:TextView? = null
    private var r: Runnable? = null
    private var r2: Runnable? = null
    private var seekBar : SeekBar? = null
    private var mDuration : Int? = null

    override fun onTrimStarted() {

    }

    override fun getResult(uri: Uri) {

        Toast.makeText(this.getActivity().getApplicationContext(), getString(R.string.video_saved_at, uri.path), Toast.LENGTH_SHORT).show()
        videoView!!.stopPlayback()
        videoView!!.setVisibility(View.GONE)
        playView!!.setVisibility(View.GONE)
        fileToReturn = uri.path
        returnEditedVideo()
    }

    override fun onError(message: String) {

        Toast.makeText(this.getActivity().getApplicationContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun cancelAction() {
        Toast.makeText(this.getActivity().getApplicationContext(), "Trim Canceled", Toast.LENGTH_SHORT).show()
        fileToReturn = filename
        videoView!!.stopPlayback()
        videoView!!.setVisibility(View.GONE)
        playView!!.setVisibility(View.GONE)
        returnEditedVideo()
    }

    var filename : String = ""

    companion object {
        fun newInstance():VideoEditor{
            return VideoEditor()
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val inflatedView: View = inflater!!.inflate(R.layout.fragment_video_editor, container, false)
        videoView = inflatedView.findViewById<VideoView>(R.id.video_loader) as VideoView
        rangeSeekBar = inflatedView.findViewById<RangeSlider>(R.id.range_slider) as RangeSlider
        seekBar = inflatedView.findViewById<SeekBar>(R.id.handlerTop) as SeekBar
        playView = inflatedView.findViewById(R.id.icon_video_play)
        tvLeft = inflatedView.findViewById<TextView>(R.id.tvLeft) as TextView
        tvRight = inflatedView.findViewById<TextView>(R.id.tvRight) as TextView

        inflatedView.findViewById<Button>(R.id.btCancel)
                .setOnClickListener(
                        { onCancelClicked() }
                )

        inflatedView.findViewById<Button>(R.id.btSave)
                .setOnClickListener( { onSaveClicked() } )


        val gestureDetector = GestureDetector(context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        onClickVideoPlayPause()
                        return true
                    }
                }
        )

        videoView!!.setOnTouchListener(View.OnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            true
        })

        return inflatedView
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        //   super.onSaveInstanceState(outState);
    }

    // when the cancel clicked, return to player with original filename
    fun onCancelClicked(){
        fileToReturn = filename
        videoView!!.stopPlayback()
        videoView!!.setVisibility(View.GONE)
        playView!!.setVisibility(View.GONE)
        returnEditedVideo()
    }

    // save the trimmed video and send the filename to player
    fun onSaveClicked(){
        Log.e(TAG, "save clicked")
        if (filename != ""){
            val mediaStorageDir = File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_MOVIES), "EasyVSD")
            startTrim(File(filename), mediaStorageDir.path, rangeSeekBar!!.getLeftIndex().toLong(), rangeSeekBar!!.getRightIndex().toLong(), this)
        }
    }

    // pause the video when clicking on the video
    private fun onClickVideoPlayPause() {
        if (videoView!!.isPlaying()) {
            playView!!.setVisibility(View.VISIBLE)
            videoView!!.pause()
            Log.d(TAG, "pause")

        } else {
            playView!!.setVisibility(View.GONE)

            Log.d(TAG, "start")
            videoView!!.start()
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            Log.d(TAG, "onAttach")
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    var mainActivity : WeakReference<MainActivity>? = null
    fun setMainActivity(main: MainActivity){
        mainActivity = WeakReference<MainActivity>(main)
    }

    //Set Video Filename for Editing
    //Set up videoView, progressbar (seekbar) and rangeSeekBar
    fun setVideoFilename(file : String){
        filename = file
        Log.d(TAG, "start_set_video_name: src: " + filename!!)
        var Uri = Uri.fromFile(File(filename))
        videoView?.setVideoURI(Uri)
        videoView!!.setVisibility(View.VISIBLE)
        playView!!.setVisibility(View.VISIBLE)

        var mediaMetadataRetriever : MediaMetadataRetriever = MediaMetadataRetriever()
        mediaMetadataRetriever.setDataSource(this.getActivity().getApplicationContext(),Uri)
        val videoLengthInMs = java.lang.Long.parseLong(mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) * 1000


        videoView!!.setOnPreparedListener(MediaPlayer.OnPreparedListener { mp ->
            // TODO Auto-generated method stub
            duration = mp.duration
            //val zero : Integer = Integer(0)

            //display time
            tvLeft?.setText("00:00:00")
            tvRight?.setText(getTime(mp.duration / 1000))

            mp.isLooping = true
            rangeSeekBar?.setTickCount(duration!!)
            seekBar?.setProgress(0)
            seekBar?.setMax(duration!!)
            rangeSeekBar?.setRangeIndex(0, duration!!)
            rangeSeekBar?.setRight(duration!!)
            rangeSeekBar?.setLeft(0)
            rangeSeekBar?.setEnabled(true)

            //rangeSeekBar Listener
            rangeSeekBar?.setRangeChangeListener( RangeSlider.OnRangeChangeListener {
                view : RangeSlider, minValue: Int, maxValue: Int ->
                videoView!!.seekTo(minValue)
                //seekBar.()
                if (videoView!!.isPlaying()) {
                    videoView!!.pause()
                    playView!!.setVisibility(View.VISIBLE)
                }

                tvLeft!!.setText(getTime(minValue/1000))

                tvRight!!.setText(getTime(maxValue/1000))

            })

            val fiveMs : Long = 5
            val handler = Handler()
            // check if the video is at the end of the selected portion
            r = Runnable {
                if (videoView!!.getCurrentPosition() == rangeSeekBar!!.getRightIndex()){
                    if(videoView!!.isPlaying()) {
                        videoView!!.pause()
                        playView!!.setVisibility(View.VISIBLE)
                    }
                }
                else if (videoView!!.getCurrentPosition() > rangeSeekBar!!.getRightIndex()){
                    videoView!!.pause()
                    playView!!.setVisibility(View.VISIBLE)
                    videoView!!.seekTo(rangeSeekBar!!.getRightIndex())
                }

                handler.postDelayed(r, fiveMs)
            }
            handler.postDelayed(r, fiveMs)

            // update progressbar per 5 ms
            r2 = Runnable {
                if (videoView!!.getCurrentPosition() > 0){
                    seekBar?.setProgress(videoView!!.getCurrentPosition())
                }
                handler.postDelayed(r2, 5)
            }
            handler.postDelayed(r2, 5)


            // seekBar change listener if the user move the seekBar
            seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                    onPlayerIndicatorSeekChanged(progress, fromUser)
                }

                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    onPlayerIndicatorSeekStart()
                }

                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    onPlayerIndicatorSeekStop(seekBar)
                }
            })
        })


        Log.d(TAG, "finish_set_video_name:" + filename!!)
    }

    //Keep the seekbar in side the rangeSeekBar
    private fun onPlayerIndicatorSeekChanged(progress: Int, fromUser: Boolean) {

        mDuration = progress

        if (fromUser) {
            if (mDuration!! < rangeSeekBar!!.getLeftIndex()) {
                mDuration = rangeSeekBar!!.getLeftIndex()
            } else if (mDuration!! > rangeSeekBar!!.getRightIndex()) {
                mDuration = rangeSeekBar!!.getRightIndex()
            }
        }
    }

    // when the user move the seek bar, video pause
    private fun onPlayerIndicatorSeekStart() {
        videoView?.pause()
        playView?.setVisibility(View.VISIBLE)
    }

    // video go to the selected position
    private fun onPlayerIndicatorSeekStop(seekBar: SeekBar) {
        videoView!!.seekTo(mDuration!!)
    }

    // return to video player
    var fileToReturn : String = ""
    fun returnEditedVideo(){
        Log.d("VideoEditor", "prepVideo: [$fileToReturn]")
        mainActivity?.get()?.showVideoPlayer(fileToReturn)
    }

    private fun getTime(seconds: Int): String {
        val hr = seconds / 3600
        val rem = seconds % 3600
        val mn = rem / 60
        val sec = rem % 60
        return String.format("%02d", hr) + ":" + String.format("%02d", mn) + ":" + String.format("%02d", sec)
    }

    //Function startTrim
    // Save the trimmed video to designated destination
    //
    // Input: a source File, destination path, trim start time, trim end time and a callback function
    //
    @Throws(IOException::class)
    private fun startTrim(src: File, dst: String, startMs: Long, endMs: Long, callback: OnTrimVideoListener) {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "MP4_$timeStamp.mp4"
        val filePath = dst + File.separator + fileName

        val file = File(filePath)
        file.parentFile.mkdirs()
        Log.d(TAG, "Generated file path " + filePath)
        genVideoUsingMp4Parser(src, file, startMs, endMs, callback)
    }

    @Throws(IOException::class)
    private fun genVideoUsingMp4Parser(src: File, dst: File, startMs: Long, endMs: Long, callback: OnTrimVideoListener) {
        // NOTE: Switched to using FileDataSourceViaHeapImpl since it does not use memory mapping (VM).
        // Otherwise we get OOM with large movie files.
        val movie = MovieCreator.build(FileDataSourceViaHeapImpl(src.absolutePath))

        val tracks = movie.tracks
        movie.tracks = LinkedList()
        // remove all tracks we will create new tracks from the old

        var startTime1 = (startMs).toDouble() / 1000.0
        var endTime1 = (endMs).toDouble() / 1000.0

        //compensate the inaccurate trimming
        if (duration!! > 15000 && endTime1 > startTime1 + 1.5){

        }

        var timeCorrected = false

        // Here we try to find a track that has sync samples. Since we can only start decoding
        // at such a sample we SHOULD make sure that the start of the new fragment is exactly
        // such a frame
        for (track in tracks) {
            if (track.syncSamples != null && track.syncSamples.size > 0) {
                if (timeCorrected) {
                    // This exception here could be a false positive in case we have multiple tracks
                    // with sync samples at exactly the same positions. E.g. a single movie containing
                    // multiple qualities of the same video (Microsoft Smooth Streaming file)

                    throw RuntimeException("The startTime has already been corrected by another track with SyncSample. Not Supported.")
                }
                startTime1 = correctTimeToSyncSample(track, startTime1, false)
                endTime1 = correctTimeToSyncSample(track, endTime1, true)
                timeCorrected = true
            }
        }

        for (track in tracks) {
            var currentSample: Long = 0
            var currentTime = 0.0
            var lastTime = -1.0
            var startSample1: Long = -1
            var endSample1: Long = -1

            for (i in 0 until track.sampleDurations.size) {
                val delta = track.sampleDurations[i]


                if (currentTime > lastTime && currentTime <= startTime1) {
                    // current sample is still before the new starttime
                    startSample1 = currentSample
                }
                if (currentTime > lastTime && currentTime <= endTime1) {
                    // current sample is after the new start time and still before the new endtime
                    endSample1 = currentSample
                }
                lastTime = currentTime
                currentTime += delta.toDouble() / track.trackMetaData.timescale.toDouble()
                currentSample++
            }
            movie.addTrack(AppendTrack(CroppedTrack(track, startSample1, endSample1)))
        }

        dst.parentFile.mkdirs()

        if (!dst.exists()) {
            dst.createNewFile()
        }

        val out = DefaultMp4Builder().build(movie)

        val fos = FileOutputStream(dst)
        val fc = fos.channel
        out.writeContainer(fc)

        fc.close()
        fos.close()
        callback.getResult(Uri.parse(dst.toString()))
    }

    //Function correctTimeToSyncSample
    // Correct time to avoid some glitches in the output video.
    // Limitation: the output video will not have the exact same length.
    private fun correctTimeToSyncSample(track: Track, cutHere: Double, next: Boolean): Double {
        val timeOfSyncSamples = DoubleArray(track.syncSamples.size)
        var currentSample: Long = 0
        var currentTime = 0.0
        for (i in 0 until track.sampleDurations.size) {
            val delta = track.sampleDurations[i]

            if (Arrays.binarySearch(track.syncSamples, currentSample + 1) >= 0) {
                // samples always start with 1 but we start with zero therefore +1
                timeOfSyncSamples[Arrays.binarySearch(track.syncSamples, currentSample + 1)] = currentTime
            }
            currentTime += delta.toDouble() / track.trackMetaData.timescale.toDouble()
            currentSample++

        }
        var previous = 0.0
        for (timeOfSyncSample in timeOfSyncSamples) {
            if (timeOfSyncSample > cutHere) {
                return if (next) {
                    timeOfSyncSample
                } else {
                    previous
                }
            }
            previous = timeOfSyncSample
        }
        return timeOfSyncSamples[timeOfSyncSamples.size - 1]
    }


    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments](http://developer.android.com/training/basics/fragments/communicating.html) for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

}// Required empty public constructor
