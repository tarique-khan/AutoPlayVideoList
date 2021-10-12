package com.tarique.autoplayvideolist

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout
import com.google.android.exoplayer2.ui.PlayerView
import com.tarique.autoplayvideolist.mediaPlayer.MediaController
import com.tarique.autoplayvideolist.mediaPlayer.MediaControllerEventListener
import com.tarique.autoplayvideolist.model.Video
import com.tarique.autoplayvideolist.util.MediaUtil

class CustomRecyclerView : RecyclerView, MediaControllerEventListener {

    private var mediaController: MediaController? = null

    private lateinit var videoSurfaceView: PlayerView
    private lateinit var videoContainer: ConstraintLayout
    private lateinit var ivThumbnail: ImageView
    private lateinit var ivPlayIcon: ImageView
    private lateinit var pbLoader: ProgressBar
    private lateinit var tvDuration: TextView

    private var addedVideo = false
    private var rowParent: View? = null
    private var playPosition = -1
    private var totalDuration = 0L

    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context)
    }

    private fun init(context: Context) {
        mediaController = MediaController(context)
        mediaController?.addEventListener(this)
        videoSurfaceView = PlayerView(context)
        videoSurfaceView.apply {
            keepScreenOn = true
            setBackgroundColor(Color.BLACK)
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            useController = false
            layoutParams = ConstraintLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                context.resources.getDimension(R.dimen.video_view_height).toInt()
            )
            transitionName = context.resources.getString(R.string.transition_name)
        }

        //Add scroll listener to play video automatically whenever the user scroll the list and get idle
        setScrollListener()

        //Add child attach and detach listener so that we can play/stop video when the child gets attached or detached
        setChildAttachDetachedListener()

        //Add global Layout change listener to play the video when the list completed rendering
        addGlobalLayoutListener()
    }

    private fun setScrollListener() {
        this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == SCROLL_STATE_IDLE) {
                    playVideo()
                }
            }
        })
    }

    private fun setChildAttachDetachedListener() {
        this.addOnChildAttachStateChangeListener(object : OnChildAttachStateChangeListener {

            override fun onChildViewAttachedToWindow(view: View) {
            }

            override fun onChildViewDetachedFromWindow(view: View) {
                if (addedVideo && rowParent != null && rowParent == view) {
                    removeVideoView()
                    mediaController?.stopVideoPlayback()
                    playPosition = -1
                }
            }

        })
    }

    private fun addGlobalLayoutListener() {
        this.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                playVideo()
                removeGlobalLayoutListener(this)
            }

        })
    }

    fun removeGlobalLayoutListener(listener: ViewTreeObserver.OnGlobalLayoutListener) {
        this.viewTreeObserver.removeOnGlobalLayoutListener(listener)
    }

    fun playVideo() {
        try {
            var targetPosition = -1

            val startPosition =
                (layoutManager as LinearLayoutManager).findFirstCompletelyVisibleItemPosition()
            val endPosition =
                (layoutManager as LinearLayoutManager).findLastCompletelyVisibleItemPosition()

            if (endPosition - startPosition == 1) {
                targetPosition = startPosition
            } else if (endPosition == startPosition && startPosition != -1) {
                targetPosition = startPosition
            }

            Log.d(TAG, "playVideo() ==> targetPosition : $targetPosition")

            // something is wrong. return.
            if (startPosition < 0 || endPosition < 0 || targetPosition == -1) {
                return
            }

            // video is already playing
            if (targetPosition == playPosition) {
                return
            }

            // set the position of the item to be played
            playPosition = targetPosition

            if (!::videoSurfaceView.isInitialized) {
                return
            }

            //Remove VideoView if it is already being played
            removeVideoView()

            val childPosition =
                targetPosition - (layoutManager as LinearLayoutManager).findFirstVisibleItemPosition()

            val child = getChildAt(childPosition) ?: return

            videoContainer = child.findViewById(R.id.clVideoListItemContainer)
            //Add VideoView
            videoContainer.addView(videoSurfaceView)
            addedVideo = true
            rowParent = child
            initializeViewFromVideoContainer()
            videoSurfaceView.requestFocus()
            videoSurfaceView.visibility =
                View.INVISIBLE //hide the VideoView until the ExoPlayer starts playing
            ivPlayIcon.visibility = View.GONE
            pbLoader.visibility = View.VISIBLE
            startVideoPlayback()
        } catch (e: Exception) {
            Log.e(TAG, "playVideo() ==> Error : ${e.message}")
        }
    }

    private fun initializeViewFromVideoContainer() {
        ivThumbnail = videoContainer.findViewById(R.id.ivVideoThumbnail)
        ivPlayIcon = videoContainer.findViewById(R.id.ivPlayIcon)
        pbLoader = videoContainer.findViewById(R.id.pbVideoLoader)
        tvDuration = videoContainer.findViewById(R.id.tvVideoDuration)
        tvDuration.bringToFront()
    }

    private fun startVideoPlayback() {
        val video = videoContainer.tag as Video
        Log.d(TAG, "startVideoPlayback() ==> Video Url : ${video.url}")
        mediaController?.startVideoPlayback(video, mute = true)
        mediaController?.attachPlayerToVideoView(videoSurfaceView)
    }

    override fun onMediaStarted(totalDuration: Long) {
        this.totalDuration = totalDuration
        videoSurfaceView.visibility = View.VISIBLE
        tvDuration.visibility = View.VISIBLE
        ivThumbnail.visibility = View.INVISIBLE
        pbLoader.visibility = View.GONE
    }

    override fun onMediaEnded() {
        removeVideoView()
        playPosition = -1
    }

    override fun onProgressChanged(position: Long) {
        val durationText =
            MediaUtil.convertMilliSecondToReadableTimeFormat(totalDuration - position)
        tvDuration.text = durationText
    }

    override fun onMediaStateChanged(state: MediaController.MediaState) {
        //Not required here
    }

    override fun onErrorOccurred(message: String?) {
        message?.let {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
        removeVideoView()
        mediaController?.stopVideoPlayback()
        playPosition = -1
    }

    private fun removeVideoView() {
        val parent = videoSurfaceView.parent as? ViewGroup
        val index = parent?.indexOfChild(videoSurfaceView) ?: -1
        if (index >= 0) {
            parent?.removeViewAt(index)
        }
        if (::ivPlayIcon.isInitialized) {
            ivPlayIcon.visibility = View.VISIBLE
        }
        if (::ivThumbnail.isInitialized) {
            ivThumbnail.visibility = View.VISIBLE
        }
        if (::tvDuration.isInitialized) {
            tvDuration.visibility = View.GONE
        }
        if (::pbLoader.isInitialized) {
            pbLoader.visibility = View.GONE
        }
        addedVideo = false
    }

    fun releasePlayer() {
        mediaController?.releasePlayer()
    }

    fun switchPlayerView() {
        if (::videoContainer.isInitialized) {
            videoContainer.addView(videoSurfaceView)
            addedVideo = true
        }
        if (::ivThumbnail.isInitialized) {
            ivThumbnail.visibility = View.INVISIBLE
        }
        if (::tvDuration.isInitialized) {
            tvDuration.visibility = View.VISIBLE
            tvDuration.bringToFront()
        }
        mediaController?.startVideoPlaybackWithDifferentPlayerView(context, videoSurfaceView)
    }

    fun stopInlineVideoPlaying() {
        mediaController?.pauseMedia()
        removeVideoView()
        playPosition = -1
    }

    fun getVideoView(): PlayerView {
        return videoSurfaceView
    }

    companion object {
        private const val TAG = "CustomRecyclerView"
    }
}