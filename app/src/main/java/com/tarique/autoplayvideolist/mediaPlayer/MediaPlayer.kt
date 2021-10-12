package com.tarique.autoplayvideolist.mediaPlayer

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory

class MediaPlayer private constructor(private val context: Context) : Player.EventListener {

    private var player: SimpleExoPlayer? = null
    private var eventHandler: MediaController.EventHandler? = null
    private val updateProgressHandler = Handler(Looper.getMainLooper())
    private var progressInterval = 1000L
    private var oldPlayerView: PlayerView? = null
    private var playerState = 0

    //used to identify the media is getting started inside onPlayerStateChanged() as it always gets called whenever user play or paused
    //so to distinguish the start of the media this flag will be used.
    //we get the media length inside onPlayerStateChanged(). so to set the duration in UI we need to provide callback from here and also it should happen for only once.
    //Hence this flag will be used.
    private var isMediaStartedPlaying = false

    init {
        player = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector())
        player?.apply {
            playWhenReady = true
            addListener(this@MediaPlayer)
        }
    }

    fun setEventHandler(handler: MediaController.EventHandler?) {
        eventHandler = handler
    }

    fun attachPlayerToVideoView(playerView: PlayerView?) {
        PlayerView.switchTargetView(player, oldPlayerView, playerView)
        player?.let {
            playerView?.player = it
            if (playerState == Player.STATE_ENDED) {
                it.seekTo(it.currentWindowIndex, C.TIME_UNSET)
            }
            oldPlayerView = playerView
        }
    }

    fun prepareMedia(videoUrl: String, lastPlayedPosition: Long) {
        val uri = Uri.parse(videoUrl)
        Log.d(TAG, "prepareMedia() ==> url : $videoUrl")
        val mediaSource = ExtractorMediaSource.Factory(
            DefaultDataSourceFactory(
                context,
                "com.tarique.autoplayvideolist"
            )
        ).createMediaSource(uri)
        player?.prepare(mediaSource)
        player?.seekTo(lastPlayedPosition)

    }

    fun play() {
        player?.playWhenReady = true
    }

    fun pause() {
        player?.playWhenReady = false
    }

    fun getTotalDuration(): Long = player?.duration ?: 0L

    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            val currentProgressPosition: Long = player?.currentPosition ?: 0
            val bundle = Bundle()
            bundle.putLong(MediaController.POSITION_CHANGED_BUNDLE_KEY, currentProgressPosition)
            sendEventMessage(MediaController.EVENT_MEDIA_POSITION_CHANGED, bundle)
            updateProgressHandler.postDelayed(this, progressInterval)
        }
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        playerState = playbackState
        when (playbackState) {

            Player.STATE_READY -> {
                if (!isMediaStartedPlaying) {
                    val bundle = Bundle().apply {
                        putSerializable(
                            MediaController.STATE_CHANGED_BUNDLE_KEY,
                            MediaController.MediaState.STARTED
                        )
                    }
                    sendEventMessage(MediaController.EVENT_MEDIA_STATE_CHANGED, bundle)
                    isMediaStartedPlaying = true
                }
                if (playWhenReady) {
                    progressUpdateRunnable.run()
                    val bundle = Bundle().apply {
                        putSerializable(
                            MediaController.STATE_CHANGED_BUNDLE_KEY,
                            MediaController.MediaState.PLAYING
                        )
                    }
                    sendEventMessage(MediaController.EVENT_MEDIA_STATE_CHANGED, bundle)
                } else {
                    val bundle = Bundle().apply {
                        putSerializable(
                            MediaController.STATE_CHANGED_BUNDLE_KEY,
                            MediaController.MediaState.PAUSED
                        )
                    }
                    sendEventMessage(MediaController.EVENT_MEDIA_STATE_CHANGED, bundle)
                }

            }

            Player.STATE_ENDED -> {
                val endedBundle = Bundle()
                updateProgressHandler.removeCallbacks(progressUpdateRunnable)
                endedBundle.putSerializable(
                    MediaController.STATE_CHANGED_BUNDLE_KEY,
                    MediaController.MediaState.ENDED
                )
                sendEventMessage(MediaController.EVENT_MEDIA_STATE_CHANGED, endedBundle)
                isMediaStartedPlaying = false
            }
        }
    }

    override fun onTracksChanged(
        trackGroups: TrackGroupArray?,
        trackSelections: TrackSelectionArray?
    ) {
        super.onTracksChanged(trackGroups, trackSelections)
        isMediaStartedPlaying = false
    }

    override fun onPlayerError(error: ExoPlaybackException?) {
        val bundle = Bundle().apply {
            putString(MediaController.ERROR_MESSAGE_BUNDLE_KEY, error?.message)
        }
        sendEventMessage(MediaController.EVENT_ERROR_OCCURRED, bundle)
        isMediaStartedPlaying = false
    }

    fun mute(mute: Boolean) {
        player?.volume = if (mute) 0f else 1f
    }

    fun stopPlayer() {
        player?.stop()
    }

    fun releasePlayer() {
        player?.stop()
        player?.release()
        player?.removeListener(this)
        updateProgressHandler.removeCallbacks(progressUpdateRunnable)
        INSTANCE = null
        player = null
        isMediaStartedPlaying = false
    }

    private fun sendEventMessage(event: Int, bundle: Bundle) {
        val message = Message().apply {
            what = event
            data = bundle
        }
        eventHandler?.sendMessage(message)
    }

    companion object {

        private const val TAG = "MediaPlayer"

        private var INSTANCE: MediaPlayer? = null

        fun getInstance(context: Context): MediaPlayer? {
            if (INSTANCE == null) {
                INSTANCE = MediaPlayer(context)
            }

            return INSTANCE
        }
    }
}