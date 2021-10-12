package com.tarique.autoplayvideolist.mediaPlayer

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.util.Log
import com.google.android.exoplayer2.ui.PlayerView
import com.tarique.autoplayvideolist.model.Video
import com.tarique.autoplayvideolist.util.MediaUtil
import java.lang.ref.WeakReference

class MediaController(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val eventHandler = EventHandler(Looper.getMainLooper(), this)
    private var eventListener: MediaControllerEventListener? = null
    private var video: Video? = null

    enum class MediaState {
        STARTED,
        PLAYING,
        PAUSED,
        ENDED,
    }

    fun addEventListener(listener: MediaControllerEventListener) {
        eventListener = listener
    }

    fun removeEventListener() {
        eventListener = null
    }

    fun startVideoPlayback(video: Video, mute: Boolean = false) {
        mediaPlayer = MediaPlayer.getInstance(context)
        mediaPlayer?.setEventHandler(eventHandler)
        mediaPlayer?.prepareMedia(
            video.url, MediaUtil.getLastPlayedPositionForVideo(
                context,
                video.id
            )
        )
        mediaPlayer?.mute(mute)
        this.video = video
    }

    fun startVideoPlaybackWithDifferentPlayerView(context: Context, playerView: PlayerView, mute: Boolean = true) {
        mediaPlayer = MediaPlayer.getInstance(context)
        mediaPlayer?.attachPlayerToVideoView(playerView)
        mediaPlayer?.mute(mute)
        mediaPlayer?.play()
    }

    fun attachPlayerToVideoView(playerView: PlayerView) {
        mediaPlayer?.attachPlayerToVideoView(playerView)
    }

    fun getTotalDuration(): Long {
        return mediaPlayer?.getTotalDuration() ?: 0L
    }

    fun stopVideoPlayback() {
        mediaPlayer?.stopPlayer()
    }

    fun playMedia() {
        mediaPlayer?.play()
    }

    fun pauseMedia() {
        mediaPlayer?.pause()
    }

    fun releasePlayer() {
        removeEventListener()
        mediaPlayer?.releasePlayer()
    }

    inner class EventHandler(looper: Looper, mediaController: MediaController): Handler(looper) {

        private var controller: WeakReference<MediaController> = WeakReference(mediaController)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            val controller = controller.get()

            when(msg.what) {
                EVENT_MEDIA_STATE_CHANGED -> {
                    val stateChangeBundle = msg.data
                    val state =
                        stateChangeBundle.getSerializable(STATE_CHANGED_BUNDLE_KEY) as MediaState
                    controller?.eventListener?.let { listener ->
                        if (state == MediaState.STARTED) {
                            listener.onMediaStarted(controller.getTotalDuration())
                        } else if (state == MediaState.ENDED) {
                            MediaUtil.storePlaybackPosition(context, video?.id ?: 0, 0)
                            listener.onMediaEnded()
                        } else {
                            listener.onMediaStateChanged(state)
                        }
                    }
                }
                EVENT_MEDIA_POSITION_CHANGED -> {
                    val positionChangeBundle = msg.data
                    val position = positionChangeBundle.getLong(POSITION_CHANGED_BUNDLE_KEY)
                    controller?.eventListener?.onProgressChanged(
                        if ((position == controller.getTotalDuration() || controller.getTotalDuration() - position <= 999)) {
                            controller.getTotalDuration()
                        } else {
                            position
                        }
                    )
                    MediaUtil.storePlaybackPosition(context, video?.id ?: 0, position)
                }

                EVENT_ERROR_OCCURRED -> {
                    val errorBundle = msg.data
                    val errorMessage = errorBundle.getString(ERROR_MESSAGE_BUNDLE_KEY)
                    controller?.eventListener?.onErrorOccurred(errorMessage)
                }
            }
        }

    }

    companion object {
        private const val TAG = "MediaController"
        const val STATE_CHANGED_BUNDLE_KEY = "state_changed_bundle_key"
        const val POSITION_CHANGED_BUNDLE_KEY = "position_changed_bundle_key"
        const val ERROR_MESSAGE_BUNDLE_KEY = "error_message_bundle_key"
        const val VIDEO_BUNDLE_KEY = "video"
        const val EVENT_MEDIA_STATE_CHANGED = 1
        const val EVENT_MEDIA_POSITION_CHANGED = 2
        const val EVENT_ERROR_OCCURRED = 3
    }
}