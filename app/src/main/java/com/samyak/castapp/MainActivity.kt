package com.samyak.castapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.mediarouter.app.MediaRouteButton
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.google.android.gms.common.images.WebImage

class MainActivity : AppCompatActivity() {
    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView
    private lateinit var castContext: CastContext
    private lateinit var castSession: CastSession
    private lateinit var sessionManager: SessionManager
    
    private var playbackPosition = 0L
    private var playWhenReady = true
    private var currentVideoUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4" // Replace with your video URL
    
    private val castSessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}
        
        override fun onSessionStarted(session: CastSession, sessionId: String) {
            castSession = session
            // Save current playback position
            playbackPosition = player.currentPosition
            // Start casting
            loadRemoteMedia(playbackPosition)
            // Optional: Pause local playback
            player.pause()
        }
        
        override fun onSessionStartFailed(session: CastSession, error: Int) {
            // Handle session start failure
        }
        
        override fun onSessionEnding(session: CastSession) {
            // Return to local playback
            if (::player.isInitialized) {
                player.seekTo(castSession.remoteMediaClient?.approximateStreamPosition ?: 0)
                player.playWhenReady = true
            }
        }
        
        override fun onSessionEnded(session: CastSession, error: Int) {}
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            castSession = session
        }
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        
        // Initialize cast context and session manager
        castContext = CastContext.getSharedInstance(this)
        sessionManager = castContext.sessionManager
        
        // Setup MediaRouteButton
        val mediaRouteButton = findViewById<MediaRouteButton>(R.id.mediaRouteButton)
        CastButtonFactory.setUpMediaRouteButton(this, mediaRouteButton)
        
        setupPlayer()
        
        // Handle system insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    
    private fun setupPlayer() {
        player = ExoPlayer.Builder(this)
            .build()
            .also { exoPlayer ->
                playerView = findViewById(R.id.playerView)
                playerView.player = exoPlayer
                
                // Load and play video using the same URL as casting
                val mediaItem = MediaItem.fromUri(currentVideoUrl)
                exoPlayer.setMediaItem(mediaItem)
                
                exoPlayer.playWhenReady = playWhenReady
                exoPlayer.seekTo(playbackPosition)
                exoPlayer.prepare()
                
                // Add player listener
                exoPlayer.addListener(playerListener)
            }
    }
    
    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> {
                    // Show loading indicator
                    playerView.keepScreenOn = true
                }
                Player.STATE_READY -> {
                    // Hide loading indicator
                    playerView.keepScreenOn = true
                }
                Player.STATE_ENDED -> {
                    playerView.keepScreenOn = false
                }
                Player.STATE_IDLE -> {
                    playerView.keepScreenOn = false
                }
            }
        }
    }
    
    private fun loadRemoteMedia(position: Long = 0) {
        if (!::castSession.isInitialized) return
        
        val remoteMediaClient = castSession.remoteMediaClient ?: return
        
        // Create media metadata
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)
        movieMetadata.putString(MediaMetadata.KEY_TITLE, "My Video")
        movieMetadata.putString(MediaMetadata.KEY_SUBTITLE, "Video Description")
        // Optional: Add thumbnail
        movieMetadata.addImage(WebImage(android.net.Uri.parse("https://your-thumbnail-url.jpg")))
        
        // Create media info
        val mediaInfo = MediaInfo.Builder(currentVideoUrl)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType("video/mp4")
            .setMetadata(movieMetadata)
            .build()
        
        // Load media
        val loadRequestData = MediaLoadRequestData.Builder()
            .setMediaInfo(mediaInfo)
            .setAutoplay(true)
            .setCurrentTime(position)
            .build()
        
        remoteMediaClient.load(loadRequestData)
            .addStatusListener { 
                // Handle load status
            }
    }
    
    override fun onStart() {
        super.onStart()
        // Initialize player when activity becomes visible
        if (!::player.isInitialized) {
            setupPlayer()
        }
    }
    
    override fun onResume() {
        super.onResume()
        sessionManager.addSessionManagerListener(castSessionManagerListener, CastSession::class.java)
        if (::player.isInitialized) {
            player.playWhenReady = playWhenReady
            player.play()
        }
    }
    
    override fun onPause() {
        super.onPause()
        sessionManager.removeSessionManagerListener(castSessionManagerListener, CastSession::class.java)
        if (::player.isInitialized) {
            playbackPosition = player.currentPosition
            playWhenReady = player.playWhenReady
            player.pause()
        }
    }
    
    override fun onStop() {
        super.onStop()
        if (::player.isInitialized) {
            player.stop()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        if (::player.isInitialized) {
            player.removeListener(playerListener)
            player.release()
        }
    }
    
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save player state
        if (::player.isInitialized) {
            outState.putLong("playback_position", player.currentPosition)
            outState.putBoolean("play_when_ready", player.playWhenReady)
        }
    }
    
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Restore player state
        playbackPosition = savedInstanceState.getLong("playback_position", 0L)
        playWhenReady = savedInstanceState.getBoolean("play_when_ready", true)
    }
}