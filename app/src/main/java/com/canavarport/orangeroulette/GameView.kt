package com.canavarport.orangeroulette

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.min

class GameView(ctx: Context) : SurfaceView(ctx), SurfaceHolder.Callback, Runnable {
    private val game = Game(ctx)
    private val snd = game.snd

    @Volatile private var running = false
    private var thread: Thread? = null
    private var surfaceReady = false

    private var scale = 1f
    private var offX = 0f
    private var offY = 0f

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    override fun surfaceCreated(h: SurfaceHolder) { surfaceReady = true; startLoop() }
    override fun surfaceChanged(h: SurfaceHolder, f: Int, w: Int, hh: Int) {
        // Рисуем в уменьшенную поверхность (~742 px по высоте), систему масштабирует на экран на GPU.
        val th = 742
        if (hh > th && (hh - th) > 8) {
            val tw = (w.toFloat() * th / hh).toInt().coerceAtLeast(1)
            h.setFixedSize(tw, th)
            return
        }
        scale = min(w.toFloat() / FP.W, hh.toFloat() / FP.H)
        offX = (w - FP.W * scale) / 2f
        offY = (hh - FP.H * scale) / 2f
    }
    override fun surfaceDestroyed(h: SurfaceHolder) { surfaceReady = false; stopLoop() }

    fun pauseGame() { stopLoop(); snd.pauseAll() }
    fun resumeGame() { snd.resumeAll(); if (surfaceReady) startLoop() }
    fun destroyGame() { stopLoop(); snd.release() }

    private fun startLoop() {
        if (running) return
        running = true
        thread = Thread(this, "game-loop").also { it.start() }
    }

    private fun stopLoop() {
        running = false
        try { thread?.join(500) } catch (e: InterruptedException) {}
        thread = null
    }

    override fun run() {
        val step = 1_000_000_000L / 40
        var next = System.nanoTime()
        while (running) {
            val now = System.nanoTime()
            var guard = 0
            while (now >= next && guard < 5) { game.tick(); next += step; guard++ }
            if (guard == 5) next = now
            val c: Canvas? = try {
                if (android.os.Build.VERSION.SDK_INT >= 26) holder.lockHardwareCanvas() else holder.lockCanvas()
            } catch (e: Exception) {
                try { holder.lockCanvas() } catch (e2: Exception) { null }
            }
            if (c != null) {
                try {
                    c.drawColor(0xFF000000.toInt())
                    c.save()
                    c.translate(offX, offY)
                    c.scale(scale, scale)
                    c.clipRect(0f, 0f, FP.W.toFloat(), FP.H.toFloat())
                    game.render(c)
                    c.restore()
                } finally {
                    try { holder.unlockCanvasAndPost(c) } catch (e: Exception) {}
                }
            }
            val sleep = (next - System.nanoTime()) / 1_000_000L
            if (sleep > 0) try { Thread.sleep(sleep) } catch (e: InterruptedException) {}
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // Касания приходят в координатах View, а не уменьшенной поверхности.
        val vs = min(width.toFloat() / FP.W, height.toFloat() / FP.H)
        val vx = (width - FP.W * vs) / 2f
        val vy = (height - FP.H * vs) / 2f
        Input.mouseX = (e.x - vx) / vs
        Input.mouseY = (e.y - vy) / vs
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> Input.pressedFlag = true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> Input.releasePending = true
        }
        return true
    }
}
