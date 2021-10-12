package com.tarique.autoplayvideolist.mediaPlayer

interface MediaControllerEventListener {
    fun onMediaStarted(totalDuration: Long)
    fun onMediaEnded()
    fun onProgressChanged(position: Long)
    fun onMediaStateChanged(state: MediaController.MediaState) //it will be either playing or paused
    fun onErrorOccurred(message: String?)
}