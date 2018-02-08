package org.invotek.apps.easyvsd_video_shell

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.TextUtils
import android.util.Log
import android.view.*
import java.io.File
import java.lang.ref.WeakReference


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [VideoPlayer.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [VideoPlayer.newInstance] factory method to
 * create an instance of this fragment.
 */
class VideoPlayer : Fragment(), View.OnClickListener {

    private var btn_newVideo : BackgroundHighlightButton? = null
    private var btn_videoRestart : BackgroundHighlightButton? = null
    private var btn_videoSkipBack : BackgroundHighlightButton? = null
    private var btn_videoPlay : BackgroundHighlightButton? = null
    private var btn_videoSkipForward : BackgroundHighlightButton? = null
    private var btn_videoPause : BackgroundHighlightButton? = null

    private var videoSurface : SurfaceView? = null
    private var videoHolder : SurfaceHolder? = null
    internal var videoPlayer: MediaPlayer? = null
    private var videoPaused: Boolean = false
    private var videoPosition: Int = 0
    private var seekToPosition: Int = 0
    private var seekComplete = false
    private var videoDuration: Int = 0
    internal var blockNavigationSelections = false
    private val videoFrameTime = 33    // ms per video frame = 33.3333 (30fps)
    private val videoCheckTime = 11    // ms per check cycle during video playback

    companion object{
        fun newInstance(): VideoPlayer {
            return VideoPlayer()
        }
    }

    private var mListener: OnFragmentInteractionListener? = null

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val inflatedView: View = inflater!!.inflate(R.layout.fragment_video_player, container, false)
        btn_newVideo = inflatedView.findViewById(R.id.btn_newPage) as BackgroundHighlightButton
        btn_newVideo?.setForegroundImageResource(mainActivity?.get(), R.drawable.new_page_circle, true)
        btn_newVideo?.setOnClickListener(this)
        btn_videoRestart = inflatedView.findViewById(R.id.btn_video_restart) as BackgroundHighlightButton
        btn_videoRestart?.setForegroundImageResource(mainActivity?.get(), R.drawable.restart_button, true)
        btn_videoRestart?.setOnClickListener(this)
        btn_videoSkipBack = inflatedView.findViewById(R.id.btn_video_skipback) as BackgroundHighlightButton
        btn_videoSkipBack?.setForegroundImageResource(mainActivity?.get(), R.drawable.skip_back_button, true)
        btn_videoSkipBack?.setOnClickListener(this)
        btn_videoPlay = inflatedView.findViewById(R.id.btn_video_play) as BackgroundHighlightButton
        btn_videoPlay?.setForegroundImageResource(mainActivity?.get(), R.drawable.play_button, true)
        btn_videoPlay?.setOnClickListener(this)
        btn_videoSkipForward = inflatedView.findViewById(R.id.btn_video_skipforward) as BackgroundHighlightButton
        btn_videoSkipForward?.setForegroundImageResource(mainActivity?.get(), R.drawable.skip_forward_button, true)
        btn_videoSkipForward?.setOnClickListener(this)
        btn_videoPause = inflatedView.findViewById(R.id.btn_video_pause) as BackgroundHighlightButton
        btn_videoPause?.setForegroundImageResource(mainActivity?.get(), R.drawable.pause_button, true)
        btn_videoPause?.setOnClickListener(this)

        videoSurface = inflatedView.findViewById(R.id.ActivityVideo) as SurfaceView
        videoHolder = videoSurface?.holder
        videoHolder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                prepVideo()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int,
                                        width: Int, height: Int) {
                prepVideo()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                if (videoPlayer != null) {
                    videoPaused = true
                    videoPlayer?.release()
                    videoPlayer = null
                }
            }
        })
        return inflatedView
    }

    private fun prepVideo() {
        Log.d("VideoPlayer", "prepVideo: [$videoFilename]")
        if (!TextUtils.isEmpty(videoFilename) && File(videoFilename).exists()) {
            if (videoPlayer == null) {
                videoPlayer = MediaPlayer.create(mainActivity?.get(), Uri.fromFile(File(videoFilename)), videoHolder)
                if (videoPlayer == null)
                // there is no video player...shouldn't happen
                else {
                    videoPaused = true
                    videoPlayer?.setLooping(false)
                    //seekToPosition = currentUser.getCurrentPage().getCurrentPausePoint().getPauseTime();
                    Log.d("PlayTalk", "prepVideo: seekToPosition=" + seekToPosition)
                    if (seekToPosition > 0) {
                        videoPlayer?.start()
                        videoPlayer?.pause()
                        videoPlayer?.seekTo(seekToPosition)
                    } else
                        videoPosition = videoPlayer?.getCurrentPosition()!!
                    videoDuration = videoPlayer?.getDuration()!!

                    val dimens = getImageSize()
                    setVideoDisplaySize(dimens)

                }

                // Setup On Completion Listener
                videoPlayer?.setOnCompletionListener(MediaPlayer.OnCompletionListener {
                    videoPaused = true
                    videoPosition = videoPlayer?.getCurrentPosition()!!

                })

                videoPlayer?.setOnInfoListener(MediaPlayer.OnInfoListener { mp, what, extra ->
                    Log.i("PlayTalk", "Main.videoPlayer.onInfoListener what=" + what +
                            " extra=" + extra)
                    false
                })

                // Setup On SeekComplete Listener
                videoPlayer?.setOnSeekCompleteListener(MediaPlayer.OnSeekCompleteListener {
                    //videoPlayer.start();
                    blockNavigationSelections = false
                    if (videoPlayer?.isPlaying()!!)
                        videoPlayer?.pause()
                    videoPaused = true
                    seekComplete = true
                    Log.i("PlayTalk", "Main.videoPlayer.onSeekComplete, videoPosition=" +
                            Integer.toString(videoPlayer?.getCurrentPosition()!!) + " seekToPosition=" +
                            seekToPosition)

                    videoPosition = videoPlayer?.getCurrentPosition()!!
                    if (videoPosition != seekToPosition) {
                        seekComplete = false
                        videoPlayer?.seekTo(seekToPosition)
                    } else {
                        // correct position has been hit
                    }
                })

                // Setup On Error Listener
                videoPlayer?.setOnErrorListener(MediaPlayer.OnErrorListener { mp, what, extra ->
                    // Log the error
                    Log.i("PlayTalk", "Main.videoPlayer.onError: ")
                    when (what) {
                        MediaPlayer.MEDIA_ERROR_UNKNOWN -> Log.i("PlayTalk", ".. What = MEDIA_ERROR_UNKNOWN")
                        MediaPlayer.MEDIA_ERROR_SERVER_DIED -> Log.i("PlayTalk", ".. What = MEDIA_ERROR_SERVER_DIED")
                        else -> Log.i("PlayTalk", ".. What = " + Integer.toString(what))
                    }
                    when (extra) {
                        MediaPlayer.MEDIA_ERROR_IO -> Log.i("PlayTalk", ".. Extra = MEDIA_ERROR_IO")
                        MediaPlayer.MEDIA_ERROR_MALFORMED -> Log.i("PlayTalk", ".. Extra = MEDIA_ERROR_MALFORMED")
                        MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> Log.i("PlayTalk", ".. Extra = MEDIA_ERROR_UNSUPPORTED")
                        MediaPlayer.MEDIA_ERROR_TIMED_OUT -> Log.i("PlayTalk", ".. Extra = MEDIA_ERROR_TIMED_OUT")
                        else -> Log.i("PlayTalk", ".. Extra = " + Integer.toString(extra))
                    }
                    // Attempt to recover from an error
                    videoPlayer?.release()
                    videoPlayer = null

                    // Tell the OS that this error has been handled.
                    true
                })
            }
        }
        return
    }

    private fun setVideoDisplaySize(dimens: IntArray) {
        // Set the size of the video SurfaceView to match the size of the
        // Imageview (destinationView)
        val videoLayoutParams = videoSurface?.getLayoutParams()!!
        videoLayoutParams.width = dimens[0]
        videoLayoutParams.height = dimens[1]
        videoSurface?.setLayoutParams(videoLayoutParams)
        //Log.i( "PlayTalk", "Main.setVideoDisplaySize: set to "
        //		+ formatIntegerArray( dimens ) );
    }

    internal var savedImageSize: IntArray? = null
    internal var imageSizeChanged = true
    fun getImageSize(): IntArray {
        val ret = intArrayOf(-1, -1)
        var iW = 0
        var iH = 0
        var rW = 0.0
        var rH = 0.0
        //		String source = "unknown";
        try {
            if (videoPlayer != null && videoSurface?.getVisibility() == View.VISIBLE) {
                ret[0] = videoSurface?.getMeasuredWidth()!!
                ret[1] = videoSurface?.getMeasuredHeight()!!
                //Log.i("PlayTalk", "Main.getImageSize1: videoSurface measuredSize=" +
                //	formatIntegerArray(ret) );

                if (videoPlayer != null) {
                    iW = videoPlayer?.getVideoWidth()!!
                    iH = videoPlayer?.getVideoHeight()!!
                    //Log.i("PlayTalk", "Main.getImageSize2: video Size=[" +
                    //	Integer.toString(iW) + "," + Integer.toString(iH) + "]");
                }
                //				source = "video";
            }

            // Scale the dimensions to preserve the aspect ratio of the original image
            if (iH > 0 && iW > 0) {
                rW = ret[0].toDouble() / iW.toDouble()
                rH = ret[1].toDouble() / iH.toDouble()
                if (rW < rH) {
                    ret[1] = Math.round(iH * rW).toInt()
                } else {
                    ret[0] = Math.round(iW * rH).toInt()
                }
            }
        } catch (ex: Exception) {
            Log.d("PlayTalk", "Main.getImageSize Error: " + ex.message)
            ex.printStackTrace()
        }

        imageSizeChanged = imageSizeChanged || savedImageSize == null || savedImageSize!![0] != ret[0] || savedImageSize!![1] != ret[1]
        savedImageSize = ret

        return ret
    }

    override fun onClick(v: View?) {
        var newPosition : Int = -1
        when(v?.id){
            R.id.btn_newPage ->{
                mainActivity?.get()?.getNewVideo()
            }
            R.id.btn_video_restart->{
                videoPaused = true
                newPosition = 0
                seekToPosition = newPosition
                videoPlayer?.seekTo(seekToPosition)
            }
            R.id.btn_video_skipback->{
                videoPaused = true
                newPosition = Math.max(videoPosition - 500, 0)
                seekToPosition = newPosition
                videoPlayer?.seekTo(seekToPosition)
            }
            R.id.btn_video_play->{
                playAndMonitorVideo()
            }
            R.id.btn_video_skipforward->{
                videoPaused = true
                newPosition = Math.min(videoPosition + 500, videoDuration)
                seekToPosition = newPosition
                videoPlayer?.seekTo(seekToPosition)
            }
            R.id.btn_video_pause->{
                videoPaused = true
                if (videoPlayer?.isPlaying()!!)
                    videoPlayer?.pause()
            }
            else->{

            }
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    fun onButtonPressed(uri: Uri) {
        if (mListener != null) {
            mListener!!.onFragmentInteraction(uri)
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
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

    var videoFilename : String = ""
    fun setFilename(file : String){
        videoFilename = file
        prepVideo()
    }

    private fun playAndMonitorVideo() {
        videoPaused = false
        videoPlayer?.start()
        Thread(Runnable {
            val stopWithinMs = videoCheckTime * 3 / 2
            //final int stopWithinMs = videoCheckTime;
            //Log.i("PlayTalk", "Main.playAndMonitorVideo: stopWithinMs = " + Integer.toString(stopWithinMs));

            // Short delay (about 100ms) to keep this routine from immediately pausing
            try {
                Thread.sleep((videoFrameTime * 3).toLong())
            } catch (e: InterruptedException) {
                Log.i("PlayTalk", "Main.playAndMonitorVideo: Could not do initial delay")
                e.printStackTrace()
            }

            while (!videoPaused) {
                try {
                    videoPosition = videoPlayer?.getCurrentPosition()!!
                    val currentPauseTime = -100 // set to a time if you want to pause the video
                    val deltaTime = currentPauseTime - videoPosition
                    Log.i("PlayTalk", "Main.playAndMonitorVideo: videoPosition=[" + videoPosition +
                            "] deltaTime=[" + deltaTime + "] stopWithinMs=[" + stopWithinMs +
                            "] videoDuration=[" + videoDuration + "] currentPauseTime=[" + currentPauseTime + "]")
                    // Each iteration of this while loop will produce a deltaTime that is
                    // ... closer to zero, and then will switch to negative values.
                    // When the value gets to be less than the stopWithinMs value the process
                    // ... will pause - even if the deltaTime value goes negative.
                    //if( (Math.abs(deltaTime) <= stopWithinMs) ) {
                    //if( (currentPauseTime != videoDuration) && (deltaTime <= stopWithinMs) ) {
                    if (Math.abs(deltaTime) <= stopWithinMs) {
                        if (videoPlayer?.isPlaying()!!)
                            videoPlayer?.pause()
                        videoPaused = true
                        var target = currentPauseTime
                        val originalTarget = target
                        do {
                            //videoPlayer.start();
                            seekToPosition = target - 100
                            seekComplete = false
                            videoPlayer?.seekTo(target)
                            while (!seekComplete) {
                                Log.i("PlayTalk", "Main.playAndMonitorVideo: thread sleeping")
                                try {
                                    Thread.sleep(100)
                                } catch (e: InterruptedException) {
                                    e.printStackTrace()
                                }

                            }
                            target -= 100
                            videoPosition = videoPlayer?.getCurrentPosition()!!
                            Log.i("PlayTalk", "Main.playAndMonitorVideo: videoPosition=" + videoPosition +
                                    " originalTarget=" + originalTarget)
                        } while (videoPosition > originalTarget &&
                                target >= videoFrameTime * 5 &&
                                Math.abs(target - videoDuration) > videoFrameTime * 5)
                        //videoPlayer.pause();
                        Log.i("PlayTalk", "Main.playAndMonitorVideo: pausing at " + Integer.toString(currentPauseTime))
                        // Adjust the pauseIndex to refer to the current PausePoint
                    }
                    Thread.sleep(videoCheckTime.toLong())
                } catch (ex: Exception) {
                    Log.i("PlayTalk", "Main.playAndMonitorVideo: Error - " + ex.message)
                }

            }
            //Log.i("PlayTalk", "Main.playAndMonitorVideo: monitor Thread ending" );
        }).start()
        //Log.i("PlayTalk", "Main.playAndMonitorVideo: exit" );
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
