package nx.screen.ds.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Movie
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.graphics.drawable.AnimatedImageDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.delay
import nx.screen.ds.data.WallpaperData
import nx.screen.ds.data.WallpaperKind
import java.io.File
import kotlin.math.roundToInt

@Composable
fun WallpaperView(
    wallpaper: WallpaperData?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    blurDp: Dp = 0.dp,
    static: Boolean = false,
    volume: Float = 1f,
) {
    if (wallpaper == null) return
    val density = LocalDensity.current
    val blurPx = remember(blurDp, density) {
        with(density) { blurDp.toPx() }.roundToInt().coerceIn(0, MAX_BLUR_PX)
    }
    when (wallpaper.kind) {
        WallpaperKind.IMAGE -> {
            if (wallpaper.image != null) {
                Image(
                    bitmap = wallpaper.image,
                    contentDescription = null,
                    contentScale = contentScale,
                    modifier = if (blurPx > 0) modifier.blur(blurDp) else modifier,
                )
            }
        }
        WallpaperKind.GIF -> {
            GifWallpaper(
                path = wallpaper.filePath,
                fallback = wallpaper.image,
                static = static,
                blurPx = blurPx,
                modifier = modifier,
            )
        }
        WallpaperKind.VIDEO -> {
            VideoWallpaper(
                path = wallpaper.filePath,
                volume = volume,
                static = static,
                blurPx = blurPx,
                modifier = modifier,
            )
        }
    }
}

private fun applyBlur(view: View, blurPx: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        view.setRenderEffect(
            if (blurPx > 0) {
                RenderEffect.createBlurEffect(
                    blurPx.toFloat(),
                    blurPx.toFloat(),
                    Shader.TileMode.MIRROR,
                )
            } else {
                null
            },
        )
    }
}

@Composable
private fun GifWallpaper(
    path: String,
    fallback: ImageBitmap?,
    static: Boolean,
    blurPx: Int,
    modifier: Modifier,
) {
    val paused = rememberLifecyclePause()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        AndroidView(
            factory = { ctx ->
                FrameLayout(ctx).apply {
                    val overlay = ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.FIT_XY
                        visibility = View.GONE
                    }
                    val gifView = ImageView(ctx).apply {
                        scaleType = ImageView.ScaleType.CENTER_CROP
                        setBackgroundColor(android.graphics.Color.BLACK)
                        val drawable = runCatching {
                            ImageDecoder.decodeDrawable(
                                ImageDecoder.createSource(File(path)),
                            ) { decoder, info, _ ->
                                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                                val maxDim = MAX_DECODE_DIMENSION
                                val largest = maxOf(info.size.width, info.size.height)
                                if (largest > maxDim) {
                                    val scale = maxDim.toFloat() / largest
                                    decoder.setTargetSize(
                                        (info.size.width * scale).toInt().coerceAtLeast(1),
                                        (info.size.height * scale).toInt().coerceAtLeast(1),
                                    )
                                }
                            }
                        }.getOrNull()
                        if (drawable is AnimatedImageDrawable) {
                            setImageDrawable(drawable)
                        } else {
                            fallback?.let { setImageBitmap(it.asAndroidBitmap()) }
                        }
                    }
                    val handler = Handler(Looper.getMainLooper())
                    val blur = BlurLoop(overlay, handler) {
                        if (!paused && !static) {
                            val w = gifView.width.coerceAtLeast(2)
                            val h = gifView.height.coerceAtLeast(2)
                            val bmp = Bitmap.createBitmap(
                                (w / 8).coerceAtLeast(1),
                                (h / 8).coerceAtLeast(1),
                                Bitmap.Config.ARGB_8888,
                            )
                            val canvas = Canvas(bmp)
                            canvas.scale(
                                (w / 8).coerceAtLeast(1) / w.toFloat(),
                                (h / 8).coerceAtLeast(1) / h.toFloat(),
                            )
                            gifView.draw(canvas)
                            overlay.setImageBitmap(bmp)
                            bmp
                        } else {
                            null
                        }
                    }
                    addView(
                        gifView,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    addView(
                        overlay,
                        FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT,
                        ),
                    )
                    tag = GifWallpaperHolder(blur)
                }
            },
            update = { view ->
                val holder = view.tag as? GifWallpaperHolder ?: return@AndroidView
                val drawable =
                    ((view as FrameLayout).getChildAt(0) as? ImageView)?.drawable as? AnimatedImageDrawable
                        ?: return@AndroidView
                if (paused || static) {
                    if (drawable.isRunning) drawable.stop()
                } else if (!drawable.isRunning) {
                    drawable.start()
                }
                if (blurPx > 0) holder.blur.start(blurPx) else holder.blur.stop()
            },
            onRelease = { view ->
                (view.tag as? GifWallpaperHolder)?.blur?.stop()
                ((view as FrameLayout).getChildAt(0) as? ImageView)?.let {
                    (it.drawable as? AnimatedImageDrawable)?.stop()
                    it.setImageDrawable(null)
                }
            },
            modifier = modifier,
        )
    } else {
        AndroidView(
            factory = { ctx -> MovieView(ctx, path) },
            update = { view ->
                applyBlur(view, blurPx)
                view.running = !paused && !static
            },
            modifier = modifier,
        )
    }
}

private class VideoPlayerHolder(
    val player: MediaPlayer,
    var volume: Float,
    val blur: BlurLoop? = null,
    val observer: LifecycleEventObserver? = null,
)

private class MusicPlayerHolder(
    val player: MediaPlayer,
    var volume: Float,
    val observer: LifecycleEventObserver,
)

private class GifWallpaperHolder(
    val blur: BlurLoop,
)

private class BlurLoop(
    val overlay: ImageView,
    private val handler: Handler,
    private val capture: () -> Bitmap?,
) {
    var radius: Int = -1
        private set
    private var last: Bitmap? = null

    val isActive: Boolean get() = radius >= 0

    fun start(blurPx: Int) {
        overlay.visibility = View.VISIBLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val r = (blurPx / 8).coerceAtLeast(1)
            if (radius != r) {
                radius = r
                overlay.setRenderEffect(
                    RenderEffect.createBlurEffect(
                        r.toFloat(),
                        r.toFloat(),
                        Shader.TileMode.MIRROR,
                    ),
                )
            }
        }
        handler.removeCallbacks(tick)
        handler.postDelayed(tick, 0)
    }

    fun stop() {
        overlay.visibility = View.GONE
        handler.removeCallbacks(tick)
        overlay.setImageDrawable(null)
        last?.recycle()
        last = null
    }

    private val tick = object : Runnable {
        override fun run() {
            runCatching {
                val bmp = capture()
                if (bmp != null) {
                    last?.recycle()
                    last = bmp
                }
            }
            handler.postDelayed(this, SNAP_MS)
        }
    }

    private companion object {
        const val SNAP_MS = 200L
    }
}

@Composable
private fun VideoWallpaper(path: String, volume: Float, static: Boolean, blurPx: Int, modifier: Modifier) {
    val paused = rememberLifecyclePause()
    val muted = rememberLifecycleMuted()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    AndroidView(
        factory = { ctx ->
            FrameLayout(ctx).apply {
                val overlay = ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_XY
                    visibility = View.GONE
                }
                val player = MediaPlayer().apply {
                    isLooping = true
                    setVolume(volume, volume)
                    setOnPreparedListener { if (!paused && !static) it.start() }
                    setOnErrorListener { _, _, _ -> true }
                    runCatching {
                        setDataSource(path)
                        prepareAsync()
                    }
                }
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY ->
                            if (player.isPlaying) player.pause()
                        Lifecycle.Event.ON_RESUME ->
                            runCatching { if (!static) player.start() }
                        else -> {}
                    }
                }
                lifecycle.addObserver(observer)
                val handler = Handler(Looper.getMainLooper())
                lateinit var textureView: TextureView
                val blur = BlurLoop(overlay, handler) {
                    if (!paused && !static) {
                        val w = textureView.width.coerceAtLeast(2)
                        val h = textureView.height.coerceAtLeast(2)
                        textureView.getBitmap(
                            (w / 8).coerceAtLeast(1),
                            (h / 8).coerceAtLeast(1),
                        )?.also { overlay.setImageBitmap(it) }
                    } else {
                        null
                    }
                }
                textureView = TextureView(ctx).apply {
                    setOpaque(false)
                    surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                            if (!blur.isActive) overlay.visibility = View.GONE
                            player.setSurface(Surface(surface))
                            if (!player.isPlaying && !paused && !static) player.start()
                        }

                        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                            runCatching {
                                val w = width.coerceAtLeast(1)
                                val h = height.coerceAtLeast(1)
                                overlay.setImageBitmap(getBitmap(w / 2, h / 2) ?: return@runCatching)
                                overlay.visibility = View.VISIBLE
                            }
                            runCatching { player.pause() }
                            return true
                        }

                        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                    }
                }
                tag = VideoPlayerHolder(player, volume, blur, observer)
                addView(
                    textureView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
                addView(
                    overlay,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            }
        },
        update = { view ->
            val holder = view.tag as? VideoPlayerHolder ?: return@AndroidView
            val player = holder.player
            val targetVolume = if (muted) 0f else volume
            if (holder.volume != targetVolume) {
                holder.volume = targetVolume
                player.setVolume(targetVolume, targetVolume)
            }
            if (paused || static) {
                if (player.isPlaying) player.pause()
            } else if (!player.isPlaying) {
                player.start()
            }
            if (blurPx > 0) holder.blur?.start(blurPx) else holder.blur?.stop()
        },
        onRelease = { view ->
            val holder = view.tag as? VideoPlayerHolder
            holder?.observer?.let { lifecycle.removeObserver(it) }
            holder?.blur?.stop()
            holder?.player?.let { mp ->
                runCatching { mp.stop() }
                mp.release()
            }
        },
        modifier = modifier,
    )
}

@Composable
fun MusicPlayer(
    path: String,
    volume: Float,
    initialPosition: Long = 0L,
    onPosition: (Long) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val paused = rememberLifecyclePause()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var playerView by remember { mutableStateOf<View?>(null) }
    AndroidView(
        factory = { ctx ->
            android.widget.FrameLayout(ctx).apply {
                val player = MediaPlayer().apply {
                    isLooping = true
                    setVolume(volume, volume)
                    setOnPreparedListener {
                        if (initialPosition > 0L) {
                            runCatching { seekTo(initialPosition.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()) }
                        }
                        if (!paused) it.start()
                    }
                    setOnErrorListener { _, _, _ -> true }
                    runCatching {
                        setDataSource(path)
                        prepareAsync()
                    }
                }
                val observer = LifecycleEventObserver { _, event ->
                    when (event) {
                        Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP, Lifecycle.Event.ON_DESTROY ->
                            if (player.isPlaying) player.pause()
                        Lifecycle.Event.ON_RESUME ->
                            runCatching { if (!paused) player.start() }
                        else -> {}
                    }
                }
                lifecycle.addObserver(observer)
                tag = MusicPlayerHolder(player, volume, observer)
            }
        },
        update = { view ->
            playerView = view
            val holder = view.tag as? MusicPlayerHolder ?: return@AndroidView
            val player = holder.player
            if (holder.volume != volume) {
                holder.volume = volume
                player.setVolume(volume, volume)
            }
            if (paused) {
                if (player.isPlaying) player.pause()
            } else if (!player.isPlaying) {
                player.start()
            }
        },
        onRelease = { view ->
            playerView = null
            val holder = view.tag as? MusicPlayerHolder
            holder?.let { h ->
                lifecycle.removeObserver(h.observer)
                runCatching { h.player.stop() }
                h.player.release()
            }
        },
        modifier = modifier,
    )
    LaunchedEffect(path) {
        while (true) {
            delay(2000)
            val holder = playerView?.tag as? MusicPlayerHolder ?: break
            val pos = runCatching { holder.player.currentPosition.toLong() }.getOrDefault(0L)
            if (pos > 0L) onPosition(pos)
        }
    }
}

@Composable
private fun rememberLifecyclePause(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var paused by remember { mutableStateOf(false) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            paused = event == Lifecycle.Event.ON_PAUSE
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return paused
}

@Composable
private fun rememberLifecycleMuted(): Boolean {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var muted by remember { mutableStateOf(lifecycle.currentState != Lifecycle.State.RESUMED) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, _ ->
            muted = lifecycle.currentState != Lifecycle.State.RESUMED
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return muted
}

private class MovieView(
    context: android.content.Context,
    path: String,
) : View(context) {
    private val movie: Movie? = runCatching { Movie.decodeFile(path) }.getOrNull()
    private val handler = Handler(Looper.getMainLooper())
    private val start = System.currentTimeMillis()
    private val tick = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, FRAME_MS)
        }
    }

    var running: Boolean = true
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                handler.post(tick)
            } else {
                handler.removeCallbacks(tick)
            }
        }

    init {
        handler.post(tick)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val m = movie ?: return
        if (m.width() <= 0 || m.height() <= 0) return
        val duration = m.duration().coerceAtLeast(1)
        m.setTime(((System.currentTimeMillis() - start) % duration).toInt())
        canvas.save()
        val scale = maxOf(width / m.width().toFloat(), height / m.height().toFloat()).coerceAtLeast(1f)
        canvas.scale(scale, scale)
        m.draw(canvas, (width - m.width() * scale) / 2f, (height - m.height() * scale) / 2f)
        canvas.restore()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacksAndMessages(null)
    }

    private companion object {
        const val FRAME_MS = 40L
    }
}

private const val MAX_BLUR_PX = 20
private const val MAX_DECODE_DIMENSION = 2048
