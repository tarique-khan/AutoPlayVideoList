package com.tarique.autoplayvideolist

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.exoplayer2.ui.PlayerControlView
import com.tarique.autoplayvideolist.mediaPlayer.MediaController
import com.tarique.autoplayvideolist.mediaPlayer.MediaControllerEventListener
import com.tarique.autoplayvideolist.model.Video
import kotlinx.android.synthetic.main.activity_video_player.*

class VideoPlayerActivity : AppCompatActivity(), MediaControllerEventListener {

    private lateinit var context: Context
    private var mediaController: MediaController? = null
    private lateinit var video: Video

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_video_player)
        context = this
        video = intent.getSerializableExtra(MediaController.VIDEO_BUNDLE_KEY) as Video
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        mediaController = MediaController(context).apply {
            addEventListener(this@VideoPlayerActivity)
        }
        setupExoPlayerControllerView()
        startVideoPlayback()
        Log.d(TAG, "onCreate: ")
    }

    private fun setupExoPlayerControllerView() {
        val controllerView = playerView.findViewById<PlayerControlView>(R.id.exo_controller)
        controllerView.setFastForwardIncrementMs(10000)
        controllerView.setRewindIncrementMs(10000)

        val closeButton = controllerView.findViewById<ImageButton>(R.id.exo_close)
        closeButton.setOnClickListener {
            finish()
        }
    }

    private fun startVideoPlayback() {
        mediaController?.startVideoPlaybackWithDifferentPlayerView(context, playerView, false)
    }
    
    override fun onPause() {
        super.onPause()
        mediaController?.pauseMedia()
    }

    override fun onMediaStarted(totalDuration: Long) {
    }

    override fun onMediaEnded() {

    }

    override fun onProgressChanged(position: Long) {

    }

    override fun onMediaStateChanged(state: MediaController.MediaState) {

    }

    override fun onErrorOccurred(message: String?) {
        message?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaController?.removeEventListener()
        mediaController?.playMedia()
    }
    
    companion object {
        private const val TAG = "VideoPlayerActivity"
    }
}