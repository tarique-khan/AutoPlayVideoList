package com.tarique.autoplayvideolist

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tarique.autoplayvideolist.model.Video

class VideoListAdapter(private val context: Context,
                       private val videoList: List<Video>): RecyclerView.Adapter<VideoListAdapter.VideoListAdapterViewHolder>() {


    interface OnItemClickListener{
        fun onItemClick(video: Video)
    }

    private var mListener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    override fun getItemCount(): Int {
        return videoList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoListAdapterViewHolder {
        return VideoListAdapterViewHolder(LayoutInflater.from(context).inflate(R.layout.layout_video_list_recycler_item, parent, false))
    }

    override fun onBindViewHolder(holder: VideoListAdapterViewHolder, position: Int) {
        val video = videoList[position]
        with(video) {
            Glide.with(context)
                .load(thumbnailUrl)
                .into(holder.ivVideoThumbnail)
            holder.tvVideoTitle.text = title
            holder.tvVideoSubTitle.text = subTitle
            holder.clVideoCotainer.tag = this
        }

        holder.itemView.setOnClickListener {
            mListener?.onItemClick(videoList[holder.adapterPosition])
        }
    }

    inner class VideoListAdapterViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {

        var ivVideoThumbnail: ImageView = itemView.findViewById(R.id.ivVideoThumbnail)
        var tvVideoTitle: TextView = itemView.findViewById(R.id.tvVideoTitle)
        var tvVideoSubTitle: TextView = itemView.findViewById(R.id.tvVideoSubTitle)
        var clVideoCotainer: ConstraintLayout = itemView.findViewById(R.id.clVideoListItemContainer)
    }
}