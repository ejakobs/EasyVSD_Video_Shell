package org.invotek.apps.easyvsd_video_shell

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import java.lang.ref.WeakReference


/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [VideoEditor.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [VideoEditor.newInstance] factory method to
 * create an instance of this fragment.
 */
class VideoEditor : Fragment() {


    private var mListener: OnFragmentInteractionListener? = null

    companion object {
        fun newInstance():VideoEditor{
            return VideoEditor()
        }
    }

//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater!!.inflate(R.layout.fragment_video_editor, container, false)
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

    var filename : String = ""
    fun setVideoFilename(file : String){
        filename = file
        // Comment out the next two lines
        fileToReturn = filename
        returnEditedVideo()
    }

    var fileToReturn : String = ""
    fun returnEditedVideo(){
        mainActivity?.get()?.showVideoPlayer(fileToReturn)
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
