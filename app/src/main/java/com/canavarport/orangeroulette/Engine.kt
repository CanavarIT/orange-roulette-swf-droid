package com.canavarport.orangeroulette

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.media.AudioAttributes
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.SoundPool
import kotlin.math.min
import kotlin.math.roundToInt

/** Мини-движок, воспроизводящий поведение FlashPunk 1.6 (фиксированные 40 тиков/сек). */
object FP {
    const val W = 700
    const val H = 495
    var camX = 0f
    var camY = 0f
    private val rng = java.util.Random()
    fun rand(n: Int): Int = if (n <= 0) 0 else (rng.nextDouble() * n).toInt()
}

object Input {
    @Volatile var mouseX = 0f      // экранные координаты (0..700, 0..495)
    @Volatile var mouseY = 0f
    @Volatile var pressedFlag = false
    @Volatile var releasePending = false
    var mousePressed = false
    fun beginFrame() { mousePressed = pressedFlag; pressedFlag = false }
}

class Anim(val name: String, val frames: IntArray, val rate: Float, val loop: Boolean)

/** Image / Spritemap / Stamp / Backdrop из FlashPunk. */
class Spr(val bmp: Bitmap, val fw: Int = bmp.width, val fh: Int = bmp.height) {
    var x = 0f
    var y = 0f
    var visible = true
    var alpha = 1f
    var flipped = false
    var angle = 0f           // как в FP: положительный = против часовой
    var scale = 1f
    var tile = false         // Backdrop(repeatX)
    var frame = 0
    var callback: (() -> Unit)? = null

    private val anims = HashMap<String, Anim>()
    private var anim: Anim? = null
    private var index = 0
    private var timer = 0f
    var complete = false
        private set
    private val cols = if (fw > 0) maxOf(1, bmp.width / fw) else 1
    private val src = Rect()
    private val dst = RectF()

    fun add(name: String, frames: IntArray, rate: Float, loop: Boolean) {
        anims[name] = Anim(name, frames, rate, loop)
    }

    fun play(name: String, reset: Boolean = false) {
        val cur = anim
        if (!reset && cur != null && cur.name == name) return
        anim = anims[name]
        val a = anim
        if (a == null) { index = 0; frame = 0; complete = true; return }
        index = 0
        timer = 0f
        frame = a.frames[0]
        complete = false
    }

    fun update() {
        val a0 = anim ?: return
        if (complete) return
        timer += a0.rate
        if (timer >= 1f) {
            while (timer >= 1f) {
                timer -= 1f
                index++
                val a = anim ?: break
                if (index == a.frames.size) {
                    if (a.loop) {
                        index = 0
                        callback?.invoke()
                    } else {
                        index = a.frames.size - 1
                        complete = true
                        callback?.invoke()
                        break
                    }
                }
            }
            val a = anim
            if (a != null && index < a.frames.size) frame = a.frames[index]
        }
    }

    fun draw(c: Canvas, px: Float, py: Float, p: Paint) {
        if (!visible) return
        val sx = fw * (frame % cols)
        val sy = fh * (frame / cols)
        if (sx >= bmp.width || sy >= bmp.height) return
        val sw = min(fw, bmp.width - sx)
        val sh = min(fh, bmp.height - sy)
        src.set(sx, sy, sx + sw, sy + sh)
        p.alpha = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
        var dx = px + x - FP.camX
        val dy = py + y - FP.camY
        if (tile) {
            dx %= fw
            if (dx > 0) dx -= fw
            var tx = dx
            while (tx < FP.W) {
                dst.set(tx, dy, tx + sw, dy + sh)
                c.drawBitmap(bmp, src, dst, p)
                tx += fw
            }
            p.alpha = 255
            return
        }
        if (angle == 0f && scale == 1f && !flipped) {
            dst.set(dx, dy, dx + sw, dy + sh)
            c.drawBitmap(bmp, src, dst, p)
        } else {
            c.save()
            c.translate(dx, dy)
            if (angle != 0f) c.rotate(-angle)
            if (scale != 1f) c.scale(scale, scale)
            if (flipped) c.scale(-1f, 1f, fw / 2f, 0f)
            dst.set(0f, 0f, sw.toFloat(), sh.toFloat())
            c.drawBitmap(bmp, src, dst, p)
            c.restore()
        }
        p.alpha = 255
    }
}

/** Flash TextField через шрифт 04B_03. */
class Txt(var text: String, var x: Float, var y: Float, val w: Int, val size: Int, val color: Int, val center: Boolean) {
    var alpha = 1f
    var visible = true
    fun draw(c: Canvas, px: Float, py: Float, tp: Paint) {
        if (!visible || alpha <= 0f || text.isEmpty()) return
        tp.textSize = size.toFloat()
        tp.color = color or 0xFF000000.toInt()
        tp.alpha = (alpha.coerceIn(0f, 1f) * 255f).roundToInt()
        val maxW = (w - 4).toFloat()
        val lines = ArrayList<String>()
        var cur = ""
        for (word in text.split(" ")) {
            val t = if (cur.isEmpty()) word else "$cur $word"
            if (tp.measureText(t) > maxW && cur.isNotEmpty()) { lines.add(cur); cur = word } else cur = t
        }
        lines.add(cur)
        val fm = tp.fontMetrics
        val lh = fm.descent - fm.ascent
        var ly = py + y - FP.camY + 2f - fm.ascent
        for (ln in lines) {
            val lx = if (center) px + x - FP.camX + 2f + (maxW - tp.measureText(ln)) / 2f else px + x - FP.camX + 2f
            c.drawText(ln, lx, ly, tp)
            ly += lh
        }
    }
}

/** Буфер размытия (BitmapData + ColorTransform alpha-множитель). */
class Blur(private val ct: Float) {
    val bmp: Bitmap = Bitmap.createBitmap(FP.W, FP.H, Bitmap.Config.ARGB_8888)
    private val cv = Canvas(bmp)
    private val fadeP = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        color = Color.argb((ct * 255f).roundToInt(), 255, 255, 255)
    }
    private val p = Paint(Paint.FILTER_BITMAP_FLAG)
    private var live = 0
    val canvas: Canvas get() = cv

    fun fade(mul: Float = ct) {
        if (live <= 0) return
        if (mul == ct) cv.drawRect(0f, 0f, FP.W.toFloat(), FP.H.toFloat(), fadeP)
        else {
            val q = Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
                color = Color.argb((mul * 255f).roundToInt(), 255, 255, 255)
            }
            cv.drawRect(0f, 0f, FP.W.toFloat(), FP.H.toFloat(), q)
        }
        live--
    }

    fun touched() { live = 160 }

    fun composite(c: Canvas) {
        if (live > 0) c.drawBitmap(bmp, 0f, 0f, p)
    }
}

open class Ent(var x: Float, var y: Float) {
    var layer = 0
    var visible = true
    val gfx = ArrayList<Any>()
    var hw = 0
    var hh = 0
    var hox = 0
    var hoy = 0
    var world: World? = null

    fun setHitbox(w: Int, h: Int, ox: Int = 0, oy: Int = 0) { hw = w; hh = h; hox = ox; hoy = oy }
    fun collidePoint(ex: Float, ey: Float, mx: Float, my: Float): Boolean =
        mx >= ex - hox && my >= ey - hoy && mx < ex - hox + hw && my < ey - hoy + hh

    open fun update() {}
    fun updateGraphics() {
        for (g in gfx) if (g is Spr) g.update()
    }

    fun drawGfx(c: Canvas, p: Paint, tp: Paint, target: Canvas = c) {
        for (g in gfx) {
            when (g) {
                is Spr -> g.draw(target, x, y, p)
                is Txt -> g.draw(target, x, y, tp)
            }
        }
    }

    open fun render(c: Canvas, p: Paint, tp: Paint) {
        drawGfx(c, p, tp)
    }
}

open class World {
    val ents = ArrayList<Ent>()
    private val adds = ArrayList<Ent>()
    private val removes = ArrayList<Ent>()
    private val sorted = ArrayList<Ent>()

    val mouseX: Float get() = Input.mouseX + FP.camX
    val mouseY: Float get() = Input.mouseY + FP.camY

    fun add(e: Ent): Ent { adds.add(e); return e }
    fun remove(e: Ent) { removes.add(e) }
    fun removeAll() { for (e in ents) removes.add(e) }

    open fun update() {
        val list = ArrayList<Ent>(ents)
        for (i in list.indices.reversed()) {          // новые сущности обновляются первыми
            val e = list[i]
            if (e.world == null) continue
            e.update()
            e.updateGraphics()
        }
    }

    fun updateLists() {
        for (e in removes) {
            if (e.world === this) { ents.remove(e); e.world = null }
            else adds.remove(e)
        }
        removes.clear()
        for (e in adds) {
            if (e.world == null) { ents.add(e); e.world = this }
        }
        adds.clear()
    }

    fun render(c: Canvas, p: Paint, tp: Paint) {
        sorted.clear()
        sorted.addAll(ents)
        sorted.sortWith(Comparator { a, b -> b.layer.compareTo(a.layer) })   // стабильная: старые раньше
        for (e in sorted) if (e.visible) e.render(c, p, tp)
    }
}

/** Картинки из assets/gfx. */
class Gfx(private val ctx: Context) {
    private val cache = HashMap<String, Bitmap>()
    private val alias = mapOf(
        "bluearm2" to "bluearm",
        "body2_body2" to "body1_body1",
        "firsty_firsty" to "hud_dark"
    )

    fun get(name: String): Bitmap {
        val n = alias[name] ?: name
        return cache.getOrPut(n) {
            val o = BitmapFactory.Options()
            o.inScaled = false
            o.inPreferredConfig = Bitmap.Config.ARGB_8888
            ctx.assets.open("gfx/$n.png").use { BitmapFactory.decodeStream(it, null, o)!! }
        }
    }
}

object A {
    lateinit var gfx: Gfx
    lateinit var snd: Snd
    lateinit var font: Typeface
}

/** Звук в стиле net.flashpunk.Sfx. */
class Sfx(private val ctx: Context, private val snd: Snd, private val file: String, private val stream: Boolean) {
    private var player: MediaPlayer? = null
    private var soundId = 0
    private var streamId = 0
    private var endAt = 0L
    private var durMs = 0L
    private var loadedOk = false
    var volume = 1f
        set(v) { field = v.coerceIn(0f, 1f); applyVol() }

    init {
        try {
            if (stream) {
                val m = MediaPlayer()
                ctx.assets.openFd("snd/$file.mp3").use { m.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                m.prepare()
                player = m
                loadedOk = true
            } else {
                ctx.assets.openFd("snd/$file.mp3").use {
                    soundId = snd.pool.load(it, 1)
                    val r = MediaMetadataRetriever()
                    r.setDataSource(it.fileDescriptor, it.startOffset, it.length)
                    durMs = (r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: "500").toLong()
                    r.release()
                }
                loadedOk = true
            }
        } catch (e: Exception) {
            loadedOk = false
        }
    }

    val playing: Boolean
        get() = if (stream) (try { player?.isPlaying == true } catch (e: Exception) { false }) else System.currentTimeMillis() < endAt

    fun play() {
        if (!loadedOk) return
        volume = 1f
        if (stream) {
            try {
                val m = player ?: return
                if (m.isPlaying) m.pause()
                m.seekTo(0)
                applyVol()
                m.start()
            } catch (e: Exception) {}
        } else {
            val v = snd.master
            streamId = snd.pool.play(soundId, v, v, 1, 0, 1f)
            endAt = System.currentTimeMillis() + durMs
        }
    }

    fun stop() {
        try {
            if (stream) { val m = player; if (m != null && m.isPlaying) { m.pause(); m.seekTo(0) } }
            else { if (streamId != 0) snd.pool.stop(streamId); endAt = 0 }
        } catch (e: Exception) {}
    }

    fun applyVol() {
        try { val v = volume * snd.master; player?.setVolume(v, v) } catch (e: Exception) {}
    }

    fun pause() { try { if (stream) player?.let { if (it.isPlaying) it.pause() } } catch (e: Exception) {} }
    fun resume() { try { if (stream) player?.start() } catch (e: Exception) {} }
    fun release() { try { player?.release() } catch (e: Exception) {}; player = null }
}

class Snd(ctx: Context) {
    val pool: SoundPool = SoundPool.Builder().setMaxStreams(8).setAudioAttributes(
        AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
    ).build()
    var master = 1f
        set(v) { field = v; all().forEach { it.applyVol() } }

    val music = Sfx(ctx, this, "music", true)
    val music2 = Sfx(ctx, this, "music2", true)
    val amon = Sfx(ctx, this, "music3", true)
    val gunshot = Sfx(ctx, this, "gunny", false)
    val gunshot2 = Sfx(ctx, this, "gunny2", false)
    val titletune = Sfx(ctx, this, "title", false)
    val trigback = Sfx(ctx, this, "setter", false)
    val clicker = Sfx(ctx, this, "clack", false)
    val here = Sfx(ctx, this, "pusher", false)
    val gunget = Sfx(ctx, this, "grabber", false)
    val woosh = Sfx(ctx, this, "burn", false)
    val spinit = Sfx(ctx, this, "clickclick", false)
    val ngdotcom = Sfx(ctx, this, "ngsound", false)

    private fun all() = listOf(music, music2, amon)

    private val wasPlaying = ArrayList<Sfx>()
    fun pauseAll() {
        wasPlaying.clear()
        for (s in all()) if (s.playing) { wasPlaying.add(s); s.pause() }
    }
    fun resumeAll() {
        for (s in wasPlaying) { s.applyVol(); s.resume() }
        wasPlaying.clear()
    }
    fun release() { all().forEach { it.release() }; pool.release() }
}
