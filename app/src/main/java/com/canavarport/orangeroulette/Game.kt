package com.canavarport.orangeroulette

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri

/** Обёртка: движок FlashPunk-мира + рендер. */
class Game(private val ctx: Context) {
    val snd = Snd(ctx)
    private val world: GameWorld
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        A.gfx = Gfx(ctx)
        A.snd = snd
        A.font = try { Typeface.createFromAsset(ctx.assets, "font/04b03.ttf") } catch (e: Exception) { Typeface.MONOSPACE }
        textPaint.typeface = A.font
        Links.opener = {
            try {
                val i = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.newgrounds.com/"))
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ctx.startActivity(i)
            } catch (e: Exception) {}
        }
        world = GameWorld()
    }

    fun tick() {
        Input.beginFrame()
        world.update()
        world.updateLists()
        if (Input.releasePending && !Input.pressedFlag) {
            Input.releasePending = false
            Input.mouseX = -1000f
            Input.mouseY = -1000f
        }
    }

    fun render(c: Canvas) {
        c.drawColor(0xFF202020.toInt())
        world.render(c, paint, textPaint)
    }
}
