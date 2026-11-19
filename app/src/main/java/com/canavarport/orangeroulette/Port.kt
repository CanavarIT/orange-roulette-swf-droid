package com.canavarport.orangeroulette

import android.graphics.Canvas
import android.graphics.Paint

/** Глобальные флаги игры (src.Global). */
object G {
    var showgunright = false; var showgunleft = false
    var click = false; var click2 = false
    var turnclick = false; var turnclick2 = false
    var firstgun = true; var firstgunoff = false
    var bullets = intArrayOf(0, 0, 0, 0, 0, 1)
    var waitforit = true; var playerturn = true; var compgo = false
    var deado = false; var deado2 = false
    var change = false; var change2 = false
    var slider = false; var gunup = false
    var shoottime = false; var risky = false; var risky2 = false
    var lastchance = false; var lastchance2 = false
    var shoo = false; var boardup = false; var boarddown = false
    var spintime = false; var live = false; var whoa = false; var ohcrap = false
    var expo = false; var gameover = false; var armup = false; var lucky = false
    var shootme = false; var limby = false; var lastround = false
    var megore = false; var fanblur = false; var dustier = false
    var shootother = false; var theend = false; var goon = false
    var shock = false; var guny = 0; var spintimes = 0
    var luckis = false; var erase = false

    fun resetvar() {
        showgunright = false; showgunleft = false; click = false; click2 = false
        turnclick = false; turnclick2 = false; firstgun = true; firstgunoff = false
        bullets = intArrayOf(0, 0, 0, 0, 0, 1)
        waitforit = true; playerturn = true; compgo = false; deado = false; deado2 = false
        change = false; change2 = false; slider = false; gunup = false; shoottime = false
        risky = false; risky2 = false; lastchance = false; lastchance2 = false
        shoo = false; boardup = false; boarddown = false; spintime = false; live = false
        whoa = false; ohcrap = false; expo = false; gameover = false; shootme = false
        lucky = false; guny = 0; limby = false; spintimes = 0; lastround = false
        megore = false; erase = false; fanblur = false; dustier = false
        shootother = false; theend = false
    }

    fun revars() {
        showgunright = false; showgunleft = false; click = false; click2 = false
        turnclick = false; turnclick2 = false; firstgun = true; firstgunoff = false
        waitforit = false; playerturn = true; compgo = false; deado = false; deado2 = false
        change = false; change2 = false; slider = false; gunup = false; shoottime = false
        risky = false; risky2 = false; lastchance = false; lastchance2 = false
        shoo = false; boardup = false; boarddown = false; spintime = false; live = false
        whoa = false; ohcrap = false; gameover = false; shootme = false; lucky = false
        lastround = false; guny = 0; spintimes = 0; megore = false; fanblur = false
        dustier = false; shootother = false
    }

    fun advance() {
        var done = false
        for (i in 0 until 6) {
            if (bullets[i] == 1 && !done) {
                bullets[i] = 0
                if (i < 5) { bullets[i + 1] = 1; done = true } else { bullets[0] = 1; done = true }
            }
        }
    }

    fun spinner(amount: Int) {
        var idx = 0
        for (i in 0 until 6) if (bullets[i] == 1) idx = i
        for (j in 0 until amount) {
            if (idx < 6) idx++
            if (idx > 5) idx = 0
        }
        for (i in 0 until 6) bullets[i] = if (i == idx) 1 else 0
    }
}

/** В оригинале HUD использует неявные глобальные `openwait` и `wait` (undefined -> NaN). */
object HudG {
    var openwait = 0.0
    var wait = 0.0
}

private fun img(n: String) = A.gfx.get(n)
private fun sm(n: String, w: Int, h: Int, cb: (() -> Unit)? = null): Spr { val s = Spr(img(n), w, h); s.callback = cb; return s }
private fun rand(n: Int) = FP.rand(n)

// ------------------------------------------------------------ статика
class Bgwall(x: Float, y: Float) : Ent(x, y) {
    init { layer = 100; gfx.add(Spr(img("bgwall_bgwall")).also { it.tile = true }) }
}
class Table(x: Float, y: Float) : Ent(x, y) { init { layer = -10; gfx.add(Spr(img("table_table"))) } }
class Body1(x: Float, y: Float) : Ent(x, y) { init { layer = -8; gfx.add(Spr(img("body1_body1"))) } }
class Body2(x: Float, y: Float) : Ent(x, y) { init { layer = -8; gfx.add(Spr(img("body2_body2")).also { it.flipped = true }) } }
class Sidewall(x: Float, y: Float) : Ent(x, y) { init { layer = 99; gfx.add(Spr(img("sidewall_sidewall"))) } }
class Sidewall2(x: Float, y: Float) : Ent(x, y) { init { layer = 99; gfx.add(Spr(img("sidewall2_sidewall2"))) } }
class Poster(x: Float, y: Float) : Ent(x, y) { init { layer = 99; gfx.add(Spr(img("poster_poster"))) } }
class Manta(x: Float, y: Float) : Ent(x, y) { init { layer = 99; gfx.add(Spr(img("manta_manta"))) } }
class Glow(x: Float, y: Float) : Ent(x, y) { init { layer = -1; gfx.add(Spr(img("glow_glow"))) } }

class Rind(x: Float, y: Float) : Ent(x, y) {
    init { layer = -100; gfx.add(Spr(img("rind_rind")).also { it.tile = true }) }
    override fun update() {
        if (G.deado || G.deado2 || G.expo) { G.boardup = false; G.boarddown = true }
        if (G.boardup && y > 415) y -= 5
        if (G.boardup && y < 416) { G.boardup = false; G.shoottime = true }
        if (G.boarddown && y < 570) y += 5
        if (G.boarddown && y > 569) G.boarddown = false
    }
}

// ------------------------------------------------------------ вентилятор, жар
class Fan(x: Float, y: Float) : Ent(x, y) {
    private val spr = Spr(img("fan_fan")).also { it.angle = 10f }
    private val blur = Blur(0.94f)
    init { layer = -2; gfx.add(spr) }
    override fun update() {
        spr.angle -= 7f
        if (spr.angle < -360f) spr.angle = 0f
    }
    override fun render(c: Canvas, p: Paint, tp: Paint) {
        blur.fade()
        if (G.fanblur) { drawGfx(c, p, tp, blur.canvas); blur.touched() } else drawGfx(c, p, tp)
        blur.composite(c)
    }
}

class Heat(x: Float, y: Float) : Ent(x, y) {
    private val spr = Spr(img("heat_heat")).also { it.alpha = 0.2f }
    private val blur = Blur(0.85f)
    init { layer = -900; gfx.add(spr) }
    override fun update() {
        if (y < -50) y = -30f
        y -= 0.1f
        if (G.fanblur) spr.alpha = 0.3f
        if (G.dustier) spr.alpha = 0.5f
        if (G.expo) spr.alpha = 0.2f
    }
    override fun render(c: Canvas, p: Paint, tp: Paint) {
        blur.fade()
        drawGfx(c, p, tp, blur.canvas); blur.touched()
        blur.composite(c)
    }
}

// ------------------------------------------------------------ экраны
class Ngbig(x: Float, y: Float) : Ent(x, y) {
    private val dark = Spr(img("ngbig_dark"))
    private val orange = Spr(img("ngbig_ngorange")).also { it.x = 200f; it.y = 100f; it.alpha = 0f }
    private val blur = Blur(0.9f)
    private var wait = 150
    private var shaker = true
    private var shake = 20f
    private var shaketimes = 20
    private var ngvocal = false
    init { layer = -160; gfx.add(dark); gfx.add(orange); setHitbox(700, 495) }
    override fun update() {
        if (!ngvocal) { A.snd.ngdotcom.play(); ngvocal = true }
        if (wait > 0) wait--
        if (shaker && orange.alpha == 1f) {
            if (shaketimes > 0) {
                shake *= -1f
                orange.y += shake
                shaketimes--
                if (shake > 0) shake -= 1f
            }
            if (shaketimes == 0) { orange.y = 100f; shaketimes = 4; shaker = false }
        }
        if (orange.alpha < 1f && wait < 130 && wait > 100) orange.alpha = (orange.alpha + 0.1f).coerceAtMost(1f)
        if (wait < 10) orange.alpha = (orange.alpha - 0.1f).coerceAtLeast(0f)
        if (collidePoint(x, y, world!!.mouseX, world!!.mouseY)) {
            if (Input.mousePressed) Links.openNewgrounds()
        }
        if (wait == 0) { G.goon = true; world!!.remove(this) }
    }
    override fun render(c: Canvas, p: Paint, tp: Paint) {
        blur.fade()
        if (shake > 0) { blur.fade(); drawGfx(c, p, tp, blur.canvas); blur.touched() } else drawGfx(c, p, tp)
        blur.composite(c)
    }
}

object Links { var opener: (() -> Unit)? = null; fun openNewgrounds() { opener?.invoke() } }

class Ngtank(x: Float, y: Float) : Ent(x, y) {
    private val spr = Spr(img("ngtank_ngtank")).also { it.scale = 0.7f }
    init { layer = -161; gfx.add(spr); setHitbox(195, 44, -20, 0) }
    override fun update() {
        if (collidePoint(x, y, Input.mouseX, Input.mouseY)) {
            y = -10f
            if (Input.mousePressed) Links.openNewgrounds()
        } else y = -8f
        if (G.expo) world!!.remove(this)
    }
}

class Ngtank2(x: Float, y: Float) : Ent(x, y) {
    init { layer = -200; gfx.add(Spr(img("ngtank2_ngtank2"))); setHitbox(179, 32) }
    override fun update() {
        x = FP.camX + 5f
        visible = !(G.expo || G.theend || G.gameover)
        if (collidePoint(x, y, world!!.mouseX, world!!.mouseY)) {
            if (Input.mousePressed) Links.openNewgrounds()
        }
    }
}

class Sounder(x: Float, y: Float) : Ent(x, y) {
    private val spr = sm("sounder_sounder", 35, 34)
    init {
        layer = -152
        spr.add("noclick", intArrayOf(1), 0f, false)
        spr.add("click", intArrayOf(0), 0f, false)
        spr.play("click")
        gfx.add(spr)
        setHitbox(35, 34)
        if (A.snd.master == 0f) spr.play("noclick")
    }
    private fun changer() {
        if (A.snd.master == 0f) { A.snd.master = 1f; spr.play("click") } else { A.snd.master = 0f; spr.play("noclick") }
    }
    override fun update() {
        if (collidePoint(x, y, world!!.mouseX, world!!.mouseY)) {
            if (Input.mousePressed) changer()
        }
        if (G.expo) world!!.remove(this)
    }
}

class Firsty(x: Float, y: Float) : Ent(x, y) {
    private val bg = Spr(img("firsty_firsty"))
    private val title = Spr(img("firsty_orangetitle"))
    private val never = sm("firsty_neverplay", 276, 199) { endo() }
    private val click = sm("firsty_click", 254, 46) { endo() }
    private var tick = 20
    private var splatted = true
    private var bong = false
    init {
        layer = -150
        never.add("splat", intArrayOf(0, 1, 2, 3, 4, 5), 0.9f, false)
        click.add("shake", intArrayOf(0, 1, 2, 1, 0), 0.5f, false)
        click.add("still", intArrayOf(0), 0f, false)
        gfx.add(bg); gfx.add(title); gfx.add(never); gfx.add(click)
        click.y = 400f; click.x = 230f
        never.y = -200f; never.x = 220f
        title.x = -5f; title.y = 160f; title.visible = false
        click.visible = false
        setHitbox(700, 495, 0, -60)
    }
    private fun endo() {
        click.play("still")
        if (splatted) { title.visible = true; click.visible = true }
    }
    override fun update() {
        if (!bong) { A.snd.titletune.play(); bong = true }
        if (tick > 0) {
            tick--
            if (tick < 2) { never.y = -10f; never.play("splat"); splatted = true }
        }
        if (rand(50) == 1) click.play("shake")
        if (collidePoint(x, y, world!!.mouseX, world!!.mouseY)) {
            if (Input.mousePressed) { G.expo = true; world!!.remove(this) }
        }
    }
}

class HUD : Ent(0f, 0f) {
    private val dark = Spr(img("hud_dark"))
    private val prison = Spr(img("hud_prison"))
    private val prison2 = Spr(img("hud_prison2"))
    private val final1 = Txt("", 0f, 200f, 500, 18, 0xFFFFFF, true)
    private val final2 = Txt("(ClicK scrEEn to ContinUe)", 0f, 200f, 500, 15, 0xFFFFFF, false)
    private var choose = rand(3)
    private var chosen = 0
    private var round = 1
    private var scary = false
    private var yack = 0
    private var yacklimit = 4
    private var firstclick = 10
    private var intro = true
    private var whelp = ""

    private val waves = arrayOf(
        "Everglades, 1972 ...", "I've been rotting here for years.", "Now they say I can leave ...", "all I have to do is win a game.",
        "San Joaquin Valley, 1934 ...", "They've squeezed us all dry.", "Now they promise to ship me out ...", "After I play a little game ...",
        "S\u00e3o Paulo, 1986 ...", "I came here to grow.", "But they picked me ...", "To play their game ...")
    private val waves2 = arrayOf(
        "I'm still boxed-in.", "They say I'm a natural ...", "One more game won't bruise me ...",
        "I'm still here.", "They say I'm not quite ready.", "One more game will ripen me up ...",
        "I'm not a blood orange.", "They won't listen.", "They're juiced for a new game.")
    private val waves3 = arrayOf(
        "Don't pick me ...", "Please don't pick me ...", "let me rot on the branch ...",
        "I'll never get out.", "I'm being stored in the dark.", "Preserved for their amusement.",
        "I'm going bad.", "I've lost all my zest ...", "Peeled and segmented.")
    private val waves4 = arrayOf(
        "I AM a blood orange.", "I can stay ripe forever ...", "I just have to concentrate ...",
        "Nothing rhymes with orange.", "We have no songs.", "We squeeze out our own fate.",
        "They keep rolling up ...", "And I turn them to pulp ...", "They keep rolling up ...")
    private val waves5 = arrayOf("I was rescued the next day.", "They said they were taking me home.", "But I am seedless and alone.")

    init {
        layer = -102
        gfx.add(dark); gfx.add(prison); gfx.add(prison2); gfx.add(final1); gfx.add(final2)
        prison.x = 450f; prison.y = 140f
        prison2.x = 480f; prison2.y = 140f
        setHitbox(700, 495)
        final1.alpha = 0f
        final2.y = 465f
        dark.y = -550f
        final2.visible = false
        prison2.alpha = 0f
        prison2.visible = false
    }

    private fun waveText(arr: Array<String>): String = arr.getOrNull(yack) ?: "undefined"

    private fun currentWave(): String = when {
        round == 1 -> waveText(waves)
        round == 2 -> waveText(waves2)
        round == 3 -> waveText(waves3)
        round == 4 -> waveText(waves4)
        else -> waveText(waves5)
    }

    private fun a(v: Float) = v.coerceIn(0f, 1f)

    override fun update() {
        if (G.spintime) { dark.alpha = 0f; prison.alpha = 0f; dark.y = -550f }
        if (!G.expo) return
        if (choose > 0) {
            chosen += if (round == 1) 4 * choose else 3 * choose
            choose = 0
            yack = chosen
        }
        if (G.expo && HudG.openwait > 0) HudG.openwait -= 1
        A.snd.music.volume = 1f
        A.snd.music.stop()
        A.snd.music2.stop()
        if (!scary) {
            whelp = currentWave()
            final1.text = whelp
            if (round > 1) yack++
            A.snd.amon.volume = 1f
            A.snd.amon.play()
            scary = true
        }
        if (!intro && dark.alpha < 1f) {
            dark.alpha = a(dark.alpha + 0.1f)
            if (round == 1) final1.alpha = a(final1.alpha + 0.1f)
        }
        final2.visible = (final1.alpha == 1f)
        if (intro) {
            whelp = waveText(waves)
            final1.text = whelp
            yack++
            dark.alpha = 1f
            dark.y = -10f
            intro = false
        }
        if (HudG.openwait > 0) return
        if (collidePoint(x, y, world!!.mouseX, world!!.mouseY) && final1.alpha > 0.9f && firstclick < 1) {
            if (Input.mousePressed) { HudG.wait = 200.0; final1.alpha = 0.9f }
        }
        dark.y = -10f
        if (firstclick > 0) firstclick--
        if (firstclick > 0) return
        if (!G.limby) { G.limby = true; G.waitforit = true }
        if (HudG.wait < 200) {
            if (final1.alpha < 1f) {
                final1.alpha = a(final1.alpha + 0.1f)
                if (round == 5) {
                    prison.visible = false
                    prison2.visible = true
                    prison2.alpha = a(prison2.alpha + 0.1f)
                }
                prison.alpha = a(prison.alpha + 0.1f)
            }
        }
        if (final1.alpha > 0f && HudG.wait > 199) final1.alpha = a(final1.alpha - 0.1f)
        if (HudG.wait > 199 && final1.alpha < 0.1f) {
            whelp = currentWave()
            final1.text = whelp
            yack++
            HudG.wait = 0.0
            yacklimit--
            if (round > 4 && yack > 3) G.theend = true
            if (yacklimit < 1 && round > 4) return
            if (yacklimit < 1) {
                final1.alpha = 0f
                firstclick = 10
                HudG.openwait = 100.0
                G.expo = false
                G.limby = false
                G.waitforit = false
                yack = 0
                yacklimit = 3
                choose = rand(3)
                chosen = 0
                round++
                if (round > 5) round = 5
                if (round > 4) choose = 0
                scary = false
                G.whoa = false
            }
        }
    }
}

class Deceased : Ent(0f, 0f) {
    private val dark2 = Spr(img("deceased_dark2"))
    private val final1 = Txt("", 0f, 200f, 700, 30, 0x000001, true)
    private val waves = arrayOf(
        "I hear her voice ...", "Knock. Knock.", "Who's there?", "Orange you glad I didn't say ...",
        "It's better this way.", "after all...", "When life gives you lemons ...", "Ha ...",
        "All I wanted was to make ...", "Vitamin C ...", "Or Penicillin ...", "Not Napalm ...")
    private var choose = rand(3)
    private var chosen = 0
    private var yack = 0
    private var yacklimit = 5
    private var firstclick = 50
    private var wait = 100
    private var waiter = 100
    private var whelp = ""
    init {
        layer = -102
        gfx.add(dark2); gfx.add(final1)
        setHitbox(700, 495)
        final1.alpha = 0f
        dark2.alpha = 0f
        dark2.y = -550f
    }
    private fun a(v: Float) = v.coerceIn(0f, 1f)
    override fun update() {
        if (choose > 1) {
            chosen += 4 * choose
            choose = 0
            yack = chosen
        }
        if (firstclick > 0) firstclick--
        if (waiter > 0 && G.gameover) waiter--
        if (waiter > 0) return
        if (G.gameover) {
            dark2.y = -10f
            if (dark2.alpha < 1f) dark2.alpha = a(dark2.alpha + 0.1f)
            G.waitforit = true
            if (wait < 100) {
                if (final1.alpha < 1f) final1.alpha = a(final1.alpha + 0.1f)
                wait++
            }
            if (final1.alpha > 0f && wait > 99) final1.alpha = a(final1.alpha - 0.1f)
            if (wait > 99 && final1.alpha < 0.1f) {
                whelp = waves.getOrNull(yack) ?: "undefined"
                final1.text = whelp
                yack++
                wait = 0
                yacklimit--
                if (yacklimit < 1) G.erase = true
            }
        }
        if (collidePoint(x, y, world!!.mouseX, world!!.mouseY) && final1.alpha > 0.9f && wait < 100) {
            if (Input.mousePressed) wait = 100
        }
    }
}

class Theend(x: Float, y: Float) : Ent(x, y) {
    private val bin = Spr(img("theend_bin"))
    private val end = Spr(img("theend_end"))
    private val face = sm("theend_endface", 88, 59)
    private var wait = 100
    init {
        layer = -160
        gfx.add(bin); gfx.add(face); gfx.add(end)
        face.add("twitch", intArrayOf(0, 1, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), 0.5f, true)
        face.play("twitch")
        face.x = 339f; face.y = 230f
        end.y = 160f; end.x = 300f
        end.alpha = 0f
        setHitbox(700, 495)
    }
    override fun update() {
        if (wait > 0) wait--
        if (wait > 0) return
        if (G.luckis) G.luckis = false
        if (end.alpha < 1f) end.alpha = (end.alpha + 0.1f).coerceAtMost(1f)
        if (collidePoint(x, y, world!!.mouseX, world!!.mouseY)) {
            if (Input.mousePressed) { G.erase = true; world!!.remove(this) }
        }
    }
}

// ------------------------------------------------------------ кровь и вспышка
class Muzzle(x: Float, y: Float) : Ent(x, y) {
    private val spr = Spr(img("muzzle_muzzle"))
    private val blur = Blur(0.8f)
    private var wait = 3
    init { layer = -1; gfx.add(spr); spr.visible = false }
    override fun update() {
        if (!G.deado && !G.deado2) { spr.flipped = false; spr.x = 0f; wait = 3 }
        if (G.deado && wait > 0) { spr.visible = true; wait-- }
        if (G.deado2 && wait > 0) { spr.x = 360f; spr.flipped = true; spr.visible = true; wait-- }
        if (wait == 0) spr.visible = false
    }
    override fun render(c: Canvas, p: Paint, tp: Paint) {
        blur.fade()
        drawGfx(c, p, tp, blur.canvas); blur.touched()
        blur.composite(c)
    }
}

class Impact(x: Float, y: Float) : Ent(x, y) {
    private val imp = Spr(img("impact_impact"), 88, 203)
    private val imp2 = Spr(img("impact_impact2"), 109, 134)
    private val gore = Spr(img("impact_gore"))
    private val gore2 = Spr(img("impact_gore2"))
    private val gore3 = Spr(img("impact_gore3"))
    private val gore4 = Spr(img("impact_gore4"))
    private val gore5 = Spr(img("impact_gore5"))
    private var drip1 = 0
    private var wait = 0
    init {
        layer = 90
        imp.add("normal", intArrayOf(0), 0f, false)
        gfx.add(imp); gfx.add(imp2); gfx.add(gore5); gfx.add(gore4); gfx.add(gore3); gfx.add(gore2); gfx.add(gore)
        imp2.x = 110f
        gore.x = (rand(10) + 10).toFloat(); gore.y = rand(20).toFloat()
        gore2.x = (rand(10) + 20).toFloat(); gore2.y = rand(20).toFloat()
        gore3.x = (rand(20) + 5).toFloat(); gore3.y = rand(30).toFloat()
        gore4.x = (rand(20) + 75).toFloat(); gore4.y = rand(30).toFloat()
        gore5.x = (rand(20) + 130).toFloat(); gore5.y = rand(30).toFloat()
        visible = false
    }
    private fun revars() {
        drip1 = rand(90) + 8
        wait = 0
        visible = false
        imp2.x = 110f
        imp.x = -17f
        gore.x = (rand(10) + 10).toFloat(); gore.y = rand(20).toFloat()
        gore2.x = (rand(10) + 20).toFloat(); gore2.y = rand(20).toFloat()
        gore3.x = (rand(20) + 5).toFloat(); gore3.y = rand(30).toFloat()
        gore4.x = (rand(20) + 75).toFloat(); gore4.y = rand(30).toFloat()
        gore5.x = (rand(20) + 130).toFloat(); gore5.y = rand(30).toFloat()
    }
    private fun drip() {
        if (drip1 > 0) {
            gore.y += 1f; gore2.y += 0.5f; gore3.y += 0.1f; gore4.y += 0.9f; gore5.y += 0.5f
            drip1--
        }
    }
    override fun update() {
        if (G.limby) revars()
        if (G.megore) { visible = true; drip() }
        if (G.deado2) {
            if (gore.x < 200f) {
                gore.x += 670f; gore2.x += 650f; gore3.x += 650f; gore4.x += 460f; gore5.x += 480f
            }
            drip()
            imp.x = 640f
            imp2.x = 550f
            imp.flipped = true
            imp2.x = 550f
            visible = true
        }
    }
}

// ------------------------------------------------------------ пистолет, барабан
class Gun(x: Float, y: Float) : Ent(x, y) {
    private val spr = Spr(img("gun_gun"))
    private var slide = 15
    private var noisy = false
    private var push = false
    init { layer = -17; gfx.add(spr); spr.visible = true }
    private fun revars() { slide = 15; push = false; noisy = false }
    override fun update() {
        if (G.limby) revars()
        if (G.showgunleft && G.lucky) { x = 210f; spr.visible = true; spr.flipped = false }
        if (G.lucky) return
        if (G.firstgun) {
            x = 230f; spr.visible = true; spr.flipped = false
            if (G.firstgunoff) { spr.visible = false; spr.flipped = true; G.firstgun = false }
        }
        if (G.showgunright) {
            if (x < 250f) x = 250f
            spr.visible = true; spr.flipped = true
            if (x < 380f) {
                if (!noisy) { A.snd.here.play(); noisy = true }
                x += slide
                if (slide > 1) slide--
            }
            if (x > 379f && !G.compgo) { noisy = false; G.compgo = true; slide = 15 }
        }
        if (G.showgunleft) {
            if (x > 300f) x = 290f
            spr.visible = true; spr.flipped = false
            if (x > 170f) {
                if (!noisy) { A.snd.here.play(); noisy = true }
                x -= slide
                if (slide > 1) slide--
            }
            if (x < 171f && !G.playerturn) { noisy = false; G.playerturn = true; slide = 15 }
        }
        if (!G.showgunright && !G.showgunleft && !G.firstgun && spr.visible) spr.visible = false
    }
}

class Cylinder(x: Float, y: Float) : Ent(x, y) {
    private val gun = Spr(img("cylinder_spingun")).also { it.x = -220f; it.y = -80f }
    private val cyl = sm("cylinder_cylinder", 380, 400)
    private val blur = Blur(0.7f)
    private var tick = 50
    private var spunned = false
    init {
        layer = -201
        cyl.add("spin", intArrayOf(0, 1, 2), 0.7f, true)
        gfx.add(gun); gfx.add(cyl)
        cyl.play("spin")
        cyl.y = 10f
    }
    override fun update() {
        if (G.compgo) { gun.flipped = true; cyl.flipped = true; cyl.x = -110f }
        else { gun.flipped = false; cyl.flipped = false; cyl.x = 0f }
        if (G.spintime) {
            cyl.visible = true
            tick--
            if (tick < 1) { G.spintime = false; tick = 50 }
        }
        if (G.spintime) {
            gun.visible = true
            if (!spunned) { A.snd.spinit.play(); spunned = true }
        } else {
            gun.visible = false
            cyl.visible = false
            spunned = false
        }
    }
    override fun render(c: Canvas, p: Paint, tp: Paint) {
        blur.fade()
        drawGfx(c, p, tp, blur.canvas); blur.touched()
        blur.composite(c)
    }
}

// ------------------------------------------------------------ кнопки
class Spin(x: Float, y: Float) : Ent(x, y) {
    private val spr = sm("spin_spin", 190, 132) { endo() }
    private var oner = 0
    private var spinamount = 0
    init {
        layer = -101
        spr.add("noclick", intArrayOf(0), 0f, false)
        spr.add("click", intArrayOf(1), 0f, false)
        spr.add("burn", intArrayOf(2, 3, 4, 5, 6, 7, 8, 9), 0.6f, false)
        spr.play("noclick")
        gfx.add(spr)
        visible = true
        setHitbox(190, 132)
    }
    private fun endo() { if (oner > 0 || G.shoo) spr.visible = false }
    override fun update() {
        if (G.limby) { oner = 0; spinamount = 0; visible = true; y = 505f }
        if (G.expo) return
        if (G.boarddown) y += 5f
        if (G.boardup && oner < 1) { y -= 5f; spr.visible = true; spr.play("noclick") }
        if (oner > 0) return
        if (G.shoo) { spr.play("burn"); return }
        if (G.shootother) return
        if (G.boarddown || G.boardup) return
        val mx = world!!.mouseX; val my = world!!.mouseY
        if (collidePoint(x, y, mx, my) && !G.spintime) {
            spr.play("click")
            if (Input.mousePressed) {
                G.spintime = true
                spinamount = rand(60) + 12
                G.spinner(spinamount)
                spr.visible = false
            }
        }
        if (!collidePoint(x, y, mx, my)) spr.play("noclick")
    }
}

class Shoot(x: Float, y: Float) : Ent(x, y) {
    private val spr = sm("shoot_shoot", 170, 132)
    private var changewait = false
    private var shotted = false
    private var wait = 0
    init {
        layer = -101
        spr.add("noclick", intArrayOf(0), 0f, false)
        spr.add("click", intArrayOf(1), 0f, false)
        spr.add("burn", intArrayOf(2, 3, 4, 5, 6, 7, 8, 9), 0.6f, false)
        spr.play("noclick")
        gfx.add(spr)
        setHitbox(170, 132)
    }
    override fun update() {
        if (G.boarddown) y += 5f
        if (G.boardup) y -= 5f
        if (G.shootother) return
        if (!G.shoottime && wait < 1) return
        val mx = world!!.mouseX; val my = world!!.mouseY
        if (collidePoint(x, y, mx, my) && !shotted) {
            spr.play("click")
            if (Input.mousePressed && !shotted) {
                spr.play("noclick")
                G.boarddown = true
                G.shoottime = false
                G.click = true
                if (G.bullets[0] == 1) {
                    G.deado = true
                    A.snd.gunshot2.play()
                    A.snd.music.stop()
                    A.snd.music2.stop()
                    shotted = true
                } else {
                    A.snd.clicker.play()
                    G.advance()
                    changewait = true
                    wait = 100
                    shotted = true
                    G.lastchance = false
                    G.live = true
                }
            }
        }
        if (changewait) {
            wait--
            if (wait < 1) { G.change = true; changewait = false; shotted = false; G.shoo = false }
        }
        if (!collidePoint(x, y, mx, my) && !shotted) spr.play("noclick")
    }
}

class Shootother(x: Float, y: Float) : Ent(x, y) {
    private val spr = sm("shootother_shootother", 190, 132) { endo() }
    private var powwait = 50
    private var wait = 0
    private var changewait = false
    private var shotted = false
    private var burnt = false
    init {
        layer = -101
        spr.add("noclick", intArrayOf(0), 0f, false)
        spr.add("click", intArrayOf(1), 0f, false)
        spr.add("burn", intArrayOf(2, 3, 4, 5, 6, 7, 8, 9), 0.6f, false)
        spr.play("noclick")
        gfx.add(spr)
        setHitbox(190, 132)
    }
    private fun endo() { if (G.shoo) visible = false }
    private fun revars() { powwait = 50; wait = 0; changewait = false; shotted = false; burnt = false }
    override fun update() {
        if (G.lastround) return
        if (G.expo) revars()
        if (G.compgo) revars()
        if (G.boarddown) y += 5f
        if (G.boardup) { y -= 5f; spr.play("noclick"); visible = true }
        if (G.boarddown || G.boardup) return
        if (G.deado) return
        if ((!G.shoottime && wait < 1) || G.lastchance) return
        if (G.shoottime && !G.shoo) visible = true
        val mx = world!!.mouseX; val my = world!!.mouseY
        if (collidePoint(x, y, mx, my) && !shotted) {
            spr.play("click")
            if (Input.mousePressed) {
                G.whoa = true
                G.risky = true
                shotted = true
                G.shootother = true
                G.shoo = true
            }
        }
        if (shotted) {
            powwait--
            if (!A.snd.woosh.playing && !burnt) {
                A.snd.woosh.play()
                spr.play("burn")
                burnt = true
            }
            if (powwait < 1 && G.shoottime) {
                G.turnclick = true
                if (G.bullets[0] == 1) {
                    A.snd.gunshot.play()
                    G.deado2 = true
                    G.shoottime = false
                    G.lucky = true
                } else {
                    A.snd.clicker.play()
                    G.advance()
                    changewait = true
                    wait = 50
                    G.ohcrap = true
                    G.shoottime = false
                }
            }
        }
        if (changewait) {
            wait--
            if (wait < 1) {
                G.lastchance = true
                G.ohcrap = false
                G.shoottime = true
                changewait = false
                shotted = false
                powwait = 50
                G.whoa = false
            }
        }
        if (!collidePoint(x, y, mx, my) && !burnt) spr.play("noclick")
    }
}

// ------------------------------------------------------------ руки
class Hand2(x: Float, y: Float) : Ent(x, y) {
    private val spr = sm("hand2_hand2", 78, 88)
    private var fisty = false
    init {
        layer = -16
        spr.add("close", intArrayOf(0, 1, 2, 3, 4), 0.5f, false)
        spr.add("open", intArrayOf(4, 3, 2, 1, 0), 0.5f, false)
        spr.add("shot", intArrayOf(5, 6, 7, 6, 5), 0.5f, false)
        spr.add("still", intArrayOf(0), 0f, false)
        gfx.add(spr)
        spr.play("still")
    }
    override fun update() {
        if (G.deado) spr.play("shot")
        if (G.deado) return
        if (!fisty && rand(100) == 1) fisty = true
        if (fisty && rand(100) == 1) fisty = false
        if (fisty) spr.play("close")
        if (!fisty) spr.play("open")
    }
}

class Hand3(x: Float, y: Float) : Ent(x, y) {
    private val spr = sm("hand3_hand3", 78, 88)
    private var fisty = false
    init {
        layer = -16
        spr.add("close", intArrayOf(0, 1, 2, 3, 4), 0.5f, false)
        spr.add("open", intArrayOf(4, 3, 2, 1, 0), 0.5f, false)
        spr.add("shot", intArrayOf(5, 6, 7, 6, 5), 0.5f, false)
        spr.add("still", intArrayOf(0), 0f, false)
        gfx.add(spr)
        spr.play("still")
        spr.flipped = true
    }
    override fun update() {
        if (G.expo) { spr.play("open"); fisty = false }
        if (G.deado2) spr.play("shot")
        if (G.deado) return
        if (!fisty && rand(2000) == 1) fisty = true
        if (fisty && rand(2000) == 1) fisty = false
        if (fisty) spr.play("close")
        if (!fisty) spr.play("open")
    }
}

private fun addArmAnims(s: Spr, up: String, down: String) {
    s.add("still", intArrayOf(0), 0.3f, false)
    s.add("return", intArrayOf(0), 0.3f, false)
    s.add("turngun", intArrayOf(25, 26, 27, 28, 29, 30), 0.4f, false)
    s.add("turntrigger", intArrayOf(29), 0f, false)
    s.add("turnback", intArrayOf(29, 28, 27, 26, 25, 12), 0.4f, false)
    s.add(up, intArrayOf(0, 1, 2, 3), 0.4f, false)
    s.add("gunlift", intArrayOf(4, 5, 6, 7, 8, 9, 10, 11, 12), 0.4f, false)
    s.add("trigger", intArrayOf(13), 0f, false)
    s.add(down, intArrayOf(18, 19, 20, 21, 22, 23), 0.4f, false)
    s.add("push", intArrayOf(15, 16, 17, 17, 17, 16, 3, 2, 1, 0), 0.4f, false)
    s.add("death", intArrayOf(31, 32, 33, 34, 35, 36, 37, 38, 39, 40), 0.5f, false)
}

class Bluearm(x: Float, y: Float) : Ent(x, y) {
    private val spr = sm("bluearm", 320, 425) { endo() }
    private var openwait = 170
    private var wait = 0
    private var wait2 = 0
    private var deadwait = 50
    private var begin = false
    private var push = false
    private var roller = false
    private var finalwait = false
    private var chango = false
    private var upper = false
    private var goforit = false
    private var dead = false
    private var shake = 5f
    private var shakechance = 3000
    private var shaketime = 0

    init {
        layer = -16
        addArmAnims(spr, "Bluearmup", "Bluearmdown")
        spr.add("gunshake", intArrayOf(11, 12, 11, 12, 11, 11, 11, 11, 11), 0.4f, false)
        spr.add("gunstill", intArrayOf(11), 0f, false)
        spr.add("setdown", intArrayOf(29, 28, 27, 26, 11, 10, 9, 8, 7, 6, 5, 4), 0.4f, false)
        spr.add("setdown2", intArrayOf(3, 2, 1, 0), 0.4f, false)
        gfx.add(spr)
        spr.play("still")
    }

    private fun revars() {
        openwait = 170; wait = 0; wait2 = 0; deadwait = 50
        begin = false; push = false; roller = false; finalwait = false
        chango = false; upper = false; goforit = false; dead = false
    }

    private fun endo() {
        if (G.spintime || G.expo) return
        if (dead) G.gameover = true
        if (goforit && !G.shock) G.shock = true
        if (G.lucky) { G.showgunleft = true; spr.play("setdown2") }
        if (dead) return
        if (G.change || G.change2) push = true
        if (chango) {
            spr.play("push"); layer = -18
            G.showgunright = true; G.playerturn = false; chango = false
        }
        if (upper && G.playerturn && !goforit && !G.lastchance && !G.click && !G.shoottime) {
            spr.play("gunlift")
            if (!G.boardup) A.snd.trigback.play()
            G.showgunleft = false; G.firstgunoff = true; G.boardup = true
        }
        if (G.playerturn && G.shoottime && !goforit) spr.play("gunstill")
        if (G.lastchance && G.shootother) G.shootother = false
    }

    override fun update() {
        if (rand(shakechance) == 1 && shaketime == 0 && !G.expo) shaketime = 5
        if (shaketime > 0) {
            FP.camY = -10f + shake
            shake *= -1f
            shaketime--
        }
        if (shaketime == 0) FP.camY = -10f
        if (G.fanblur) { shakechance = 500; shake = 7f }
        if (G.dustier) { shakechance = 300; shake = 8f }
        if (G.deado2 && !G.expo) { revars(); shaketime = 5; G.expo = true }
        if (G.limby) spr.play("still")
        if (G.limby) return
        if (G.deado && !dead) {
            if (G.playerturn) spr.play("death")
            dead = true
            layer = -9
            shaketime = 5
        }
        if (G.turnclick) { spr.play("turntrigger"); G.turnclick = false }
        if (dead) return
        if (G.compgo) layer = -16
        if (G.lucky) deadwait--
        if (G.lucky && deadwait < 1 && !G.showgunleft) spr.play("setdown")
        if (G.deado || G.deado2) return
        if (wait2 > 1 && G.playerturn) wait2--
        if (wait2 > 1) return
        if (wait > 0) wait--
        if (wait != 0) return
        if (!begin) {
            if (openwait > 0) openwait--
            if (openwait < 1 && !begin) begin = true
        }
        if (!begin) return
        if (G.risky) { spr.play("turngun"); goforit = true; G.risky = false; wait = 50 }
        if (G.lastchance && goforit) { spr.play("turnback"); goforit = false }
        if (goforit) return
        if (G.change2) return
        if (!G.playerturn) { upper = false; wait2 = 50 }
        if (G.change) {
            G.click = false; G.live = false
            spr.play("Bluearmdown")
            chango = true
            G.armup = false
            G.change = false
        }
        if (G.change) return
        if (G.playerturn && !upper) { spr.play("Bluearmup"); G.armup = true; upper = true }
        if (G.playerturn && G.shoottime && !G.risky && rand(100) == 1) spr.play("gunshake")
        if (G.click && !G.risky) spr.play("trigger")
    }
}

class Bluearm2(x: Float, y: Float) : Ent(x, y) {
    private val spr = sm("bluearm2", 320, 425) { endo() }
    private var openwait = 100
    private var wait = 0
    private var wait2 = 0
    private var begin = false
    private var push = false
    private var finalwait = false
    private var chango = false
    private var upper = false
    private var gunisup = false
    private var roller = false
    private var dead = false
    private var turn = 0
    private var oner = 0
    private var spinamount = 0
    private var foe = 0
    private var choice = rand(6) + 4
    private var lastchance = false
    private var goforit = false
    private var surprise = true
    private var nono = false

    init {
        layer = -16
        addArmAnims(spr, "Bluearm2up", "Bluearm2down")
        gfx.add(spr)
        spr.play("still")
        spr.flipped = true
    }

    private fun revars() {
        openwait = 100; wait = 0; wait2 = 0; begin = false; push = false
        finalwait = false; chango = false; upper = false; gunisup = false
        roller = false; dead = false; turn = 0; oner = 0; spinamount = 0
        lastchance = false; goforit = false; surprise = true; nono = false
    }

    private fun endo() {
        if (dead) return
        if (G.change || G.change2) push = true
        if (chango) { spr.play("push"); layer = -18; G.showgunleft = true; chango = false }
        if (upper && !G.playerturn && !goforit && !lastchance) {
            spr.play("gunlift")
            if (G.showgunright) A.snd.trigback.play()
            G.showgunright = false
            roller = true
        }
        if (upper && !G.playerturn && goforit) { wait = 20; G.showgunright = false; roller = true }
    }

    override fun update() {
        val snd = A.snd
        if (!snd.music.playing && turn < 2 && !G.expo) snd.music.play()
        if (!snd.music2.playing && turn > 1 && !G.deado && !G.deado2) { snd.music2.play(); G.fanblur = true }
        if (snd.music2.playing && snd.music.volume > 0f) snd.music.volume = snd.music.volume - 0.1f
        if (snd.music.playing && snd.amon.volume > 0f) snd.amon.volume = snd.amon.volume - 0.1f
        if (G.limby) revars()
        if (G.playerturn) layer = -16
        if (G.deado2 && !dead) {
            if (G.compgo) spr.play("death")
            foe++
            choice--
            dead = true
            layer = -9
        }
        if (dead) return
        if (G.expo) return
        if (G.deado || G.deado2) return
        if (G.playerturn) spr.play("still")
        if (wait > 0 && !G.playerturn) wait--
        if (wait2 > 0 && G.compgo) wait2--
        if (wait2 > 0) return
        if (wait != 0) return
        if (!begin) {
            if (!G.playerturn) {
                if (openwait > 0) openwait--
                if (openwait < 1) begin = true
            }
        }
        if (!begin) return
        if (G.limby) spr.play("still")
        if (G.change2 && wait < 1) {
            spr.play("Bluearm2down")
            chango = true
            G.click2 = false
            G.compgo = false
            G.change2 = false
            G.live = false
            wait2 = 100
            upper = false
        }
        if (G.compgo && !upper && !G.playerturn && wait2 < 1 && !G.click2) {
            spr.play("Bluearm2up")
            upper = true
            wait = 10
        }
        if (wait > 0) return
        if (!G.playerturn && G.compgo) {
            gunisup = true
            if (gunisup && roller) { roller = false; wait = 50 }
            if (gunisup && wait < 1) {
                if (turn > 1 && oner < 1 && !lastchance) {
                    if (rand(2) == 1) {
                        G.spintime = true
                        spinamount = rand(60) + 12
                        G.spinner(spinamount)
                        nono = true
                        wait = 140
                    }
                }
                if (!nono) {
                    if ((rand(choice) + 1) == 1 && surprise) { G.risky2 = true; surprise = false }
                    if (G.risky2 && !goforit && upper) {
                        spr.play("turngun")
                        goforit = true
                        lastchance = true
                        G.risky2 = false
                        G.shootme = true
                        wait = 50
                    }
                    if (G.lastchance2 && goforit) {
                        spr.play("turnback")
                        goforit = false
                        wait = 30
                    }
                    if (goforit && wait < 1) {
                        if (G.bullets[0] == 1) {
                            spr.play("turntrigger")
                            snd.gunshot2.play()
                            snd.music.stop()
                            snd.music2.stop()
                            G.deado = true
                            G.gameover = true
                            G.shootme = false
                        } else {
                            snd.clicker.play()
                            spr.play("turntrigger")
                            G.advance()
                            G.lastchance2 = true
                            G.shootme = false
                            lastchance = true
                            wait = 50
                        }
                    }
                }
                if (goforit) return
                if (G.lastround) visible = false
                if (G.lastround) return
                if (!G.spintime && !finalwait) { finalwait = true; wait = 100 }
                if (wait < 1 && finalwait) {
                    G.click2 = true
                    spr.play("trigger")
                    if (G.bullets[0] == 1) {
                        snd.gunshot.play()
                        G.deado2 = true
                    } else {
                        snd.clicker.play()
                        G.advance()
                        gunisup = false
                        G.change2 = true
                        G.spintimes = 0
                        G.live = true
                        roller = true
                        wait = 50
                        finalwait = false
                        turn++
                        lastchance = false
                        G.lastchance2 = false
                        G.turnclick = false
                        surprise = true
                        nono = false
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------ головы
class Head1(x: Float, y: Float) : Ent(x, y) {
    private val spike = sm("head1_spikey", 225, 212) { }
    private val face = sm("head1_face1", 151, 139) { }
    private val blur = Blur(0.6f)
    private var shake = 10
    private var wait = 0
    private var dead = false
    private var looking = true
    private var tense = false
    private var nextscare = false
    private var twitch = false

    init {
        layer = -11
        face.add("still", intArrayOf(0), 0.4f, false)
        face.add("croaked", intArrayOf(1, 1, 2, 3, 4, 5, 6, 7, 8), 0.5f, false)
        face.add("lookdown", intArrayOf(14, 15, 16, 17, 18, 19, 20, 40, 41, 42, 41, 42, 41, 42, 41), 0.4f, false)
        face.add("lookdown2", intArrayOf(43, 42, 43, 42, 43, 42, 43, 42, 43), 0.4f, false)
        face.add("lookdown3", intArrayOf(41), 0f, false)
        face.add("scared", intArrayOf(22, 21, 20, 21, 22, 23), 0.4f, false)
        face.add("scared2", intArrayOf(24, 25, 26, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 27, 26, 25, 24), 0.4f, false)
        face.add("scared3", intArrayOf(16, 17, 18, 19, 18, 17, 16), 0.4f, false)
        face.add("notyou", intArrayOf(28, 29, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 38, 29, 44, 45, 46, 47, 48, 49, 50), 0.4f, false)
        face.add("twitch", intArrayOf(22, 23, 24, 23, 22, 22, 22, 22, 22, 22, 22, 22), 0.4f, false)
        face.add("click", intArrayOf(28, 29, 29, 29, 21, 22, 23), 0.4f, false)
        face.add("uhoh", intArrayOf(28, 29, 38, 39, 39, 39, 39, 39, 39, 39, 39, 39, 39, 23, 22, 21, 20), 0.4f, false)
        face.add("lookother", intArrayOf(9, 10, 11, 12, 13, 14), 0.4f, false)
        face.add("lookother2", intArrayOf(13, 14, 13, 14, 13, 14, 13, 14), 0.4f, false)
        face.add("evil", intArrayOf(31, 32, 33, 34, 35), 0.4f, false)
        face.add("evil2", intArrayOf(34, 35, 34, 35, 34, 34, 34, 34), 0.4f, false)
        spike.add("normal", intArrayOf(0), 0f, false)
        spike.add("juice", intArrayOf(2, 3, 4, 5, 6, 3, 8, 5), 0.5f, false)
        gfx.add(spike); gfx.add(face)
        face.x = 47f; face.y = 74f
        face.play("lookdown")
    }

    private fun revars() { shake = 10; wait = 0; dead = false; looking = false; tense = false; nextscare = false }

    override fun update() {
        if (G.limby) revars()
        if (G.deado && !dead) {
            face.x = 37f
            spike.x = -15f
            spike.play("juice")
            face.play("croaked")
            G.megore = true
            dead = true
        }
        if (G.live) { face.play("click"); looking = false }
        if (G.live) return
        if (wait > 1) wait--
        if (wait > 1) return
        if (G.deado2 && !dead) { face.play("notyou"); dead = true }
        if (dead) return
        if (G.ohcrap) { face.play("uhoh"); G.whoa = false; tense = false }
        if (G.ohcrap) return
        if (G.showgunleft && !G.armup && !looking) { face.play("lookdown"); looking = true }
        if (G.firstgun && rand(50) == 1 && twitch) { face.play("lookdown3"); twitch = false }
        if (G.firstgun && rand(50) == 1 && !twitch) { face.play("lookdown2"); twitch = true }
        if (!G.live && G.armup) {
            if (G.firstgunoff && G.playerturn && !G.whoa && rand(100) == 1) { face.play("scared"); wait = 30 }
            if (G.firstgunoff && G.playerturn && !G.whoa && rand(200) == 1) { face.play("scared2"); nextscare = true; wait = 30 }
            if (G.firstgunoff && G.playerturn && !G.whoa && rand(100) == 1) { face.play("scared3"); nextscare = false; wait = 30 }
            if (G.firstgunoff && G.playerturn && !G.whoa && rand(100) == 1) { face.play("twitch"); nextscare = true; wait = 30 }
        }
        if (G.showgunright && G.compgo && !G.live && !looking) { face.play("lookother"); looking = true }
        if (G.showgunright && G.compgo && !G.live && rand(100) == 1) face.play("lookother2")
        if (G.compgo && face.x < 55f) face.x += 2f
        if (G.playerturn && face.x > 47f) face.x -= 2f
        if (G.whoa && !tense && !G.ohcrap) { face.play("evil"); tense = true }
        if (tense && rand(50) == 1 && !G.ohcrap) face.play("evil2")
    }

    override fun render(c: Canvas, p: Paint, tp: Paint) {
        blur.fade()
        if (G.deado) { drawGfx(c, p, tp, blur.canvas); blur.touched() } else drawGfx(c, p, tp)
        blur.composite(c)
    }
}

class Head2(x: Float, y: Float) : Ent(x, y) {
    private val spike = sm("head2_spikey", 228, 220)
    private val f2 = sm("head2_face2", 159, 137) { endo() }
    private val f3 = sm("head2_face3", 159, 137) { endo() }
    private val f4 = sm("head2_face4", 159, 137) { endo() }
    private val f5 = sm("head2_face5", 120, 137) { endo() }
    private val blur = Blur(0.9f)
    private var changeo = false
    private var dead = false
    private var deadflash = false
    private var flasher = false
    private var liver = false
    private var looky = false
    private var scary = false
    private var shake = 7
    private var shaker = 0
    private var wait = 0
    private var whichface = 0

    init {
        for (f in arrayOf(f2, f3, f4)) {
            f.add("croaked", intArrayOf(0, 0, 1, 2, 3, 4, 5, 6), 0.5f, false)
            f.add("lookdown", intArrayOf(11, 12, 13), 0.4f, false)
            f.add("lookother", intArrayOf(7, 8), 0.4f, false)
            f.add("lookother2", intArrayOf(7), 0.4f, false)
            f.add("scared", intArrayOf(11, 7, 7, 8, 9), 0.4f, false)
            f.add("scared2", intArrayOf(7, 8, 7, 8, 7, 8, 7, 8, 7, 8), 0.4f, false)
            f.add("click", intArrayOf(14, 15, 15, 15, 15, 15, 14, 7), 0.5f, false)
            f.add("shootem", intArrayOf(16, 17), 0.5f, false)
            f.add("deadface", intArrayOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18), 0.9f, false)
        }
        f2.add("scared2", intArrayOf(7, 8, 7, 8, 7, 8, 7, 8, 7, 8), 0.5f, false)
        f3.add("firstlook", intArrayOf(17), 0.4f, false)
        f5.add("still", intArrayOf(0, 1, 2), 0.5f, true)
        spike.add("normal", intArrayOf(0), 0f, false)
        spike.add("juice", intArrayOf(1, 3, 4, 5, 6, 7, 8, 9), 0.5f, false)
        layer = -11
        gfx.add(spike); gfx.add(f2); gfx.add(f3); gfx.add(f4); gfx.add(f5)
        f2.play("lookother")
        f2.x = 20f; f2.y = 75f
        f3.x = 20f; f3.y = 72f
        f4.x = 20f; f4.y = 74f
        f5.x = 30f; f5.y = 74f
        f2.visible = true; f3.visible = false; f4.visible = false; f5.visible = false
        f5.play("still")
    }

    private fun each(name: String) { f2.play(name); f3.play(name); f4.play(name) }

    private fun endo() {
        if (G.playerturn && !dead && !G.whoa && changeo && !flasher) each("lookother")
        if (G.shock && scary) { G.shock = false; scary = false }
        if (whichface == 2) changeo = true
        if (flasher) {
            if (f2.x < 0f) { f2.x = 20f; f2.y = 75f; f2.visible = false; f2.play("lookother") }
            if (f3.x < 0f) { f3.x = 20f; f3.y = 72f; f3.visible = false; f3.play("lookother") }
            if (f4.x < 0f) { f4.x = 20f; f4.y = 74f; f4.visible = false; f4.play("lookother") }
            flasher = false
        }
    }

    private fun revars() {
        shake = 7; shaker = 5; dead = false; wait = 0
        liver = false; looky = false
    }

    override fun update() {
        if (rand(100) == 1 && shaker == 0 && !G.expo && whichface == 3) shaker = 5
        if (shaker > 0) { x = 490f + shake; shake *= -1; shaker-- }
        if (shaker == 0) x = 490f
        if (G.limby) {
            revars()
            if (whichface == 1) { f2.visible = false; f3.visible = true; deadflash = true }
            if (whichface == 2) { f2.visible = false; f3.visible = false; f4.visible = true }
            if (whichface == 3) { f2.visible = false; f3.visible = false; f4.visible = false; f5.visible = true; G.dustier = true }
            spike.play("normal")
            f2.play("lookother")
            f3.play("firstlook")
            f4.play("lookother")
            f2.x = 20f; f3.x = 20f; f4.x = 20f; f5.x = 30f
            spike.x = 0f
            scary = false
            flasher = false
        }
        if (G.deado) {
            if (whichface > 1) { f2.visible = false; f3.visible = false; f4.visible = false; f5.visible = true }
            if (whichface < 2) { f2.play("shootem"); f3.play("shootem"); whichface = 0 }
            dead = true
        }
        if (G.deado2 && !dead) {
            f2.x = 33f; f3.x = 33f; f4.x = 33f
            spike.x = 10f
            each("croaked")
            f5.visible = false
            spike.play("juice")
            whichface++
            dead = true
        }
        if (dead) return
        if (G.playerturn) {
            if (deadflash && whichface == 1) {
                if (!flasher && rand(1000) == 1) {
                    flasher = true; f2.visible = true; f2.x = -180f; f2.y = 70f; f2.alpha = 0.3f; f2.play("deadface")
                }
            }
            if (deadflash && whichface == 2) {
                if (!flasher && rand(700) == 1) {
                    flasher = true; f2.visible = true; f2.x = -180f; f2.y = 70f; f2.alpha = 0.3f; f2.play("deadface")
                }
                if (!flasher && rand(700) == 1) {
                    flasher = true; f3.visible = true; f3.x = -270f; f3.y = -50f; f3.alpha = 0.3f; f3.play("deadface")
                }
            }
            if (deadflash && whichface == 3) {
                if (!flasher && rand(500) == 1) {
                    flasher = true; f2.visible = true; f2.x = -180f; f2.y = 70f; f2.alpha = 0.3f; f2.play("deadface")
                }
                if (!flasher && rand(500) == 1) {
                    flasher = true; f3.visible = true; f3.x = -75f; f3.y = -50f; f3.alpha = 0.3f; f3.play("deadface")
                }
                if (!flasher && rand(500) == 1) {
                    flasher = true; f4.visible = true; f4.x = -270f; f4.y = -50f; f4.alpha = 0.3f; f4.play("deadface")
                }
            }
        }
        if (G.compgo && f5.x < 30f) { f2.x += 2f; f3.x += 2f; f4.x += 2f; f5.x += 2f }
        if (G.playerturn && f5.x > 25f) { f2.x -= 2f; f3.x -= 2f; f4.x -= 2f; f5.x -= 2f }
        if (wait > 1) wait--
        if (wait > 1) return
        if (G.shock && !scary) { each("click"); scary = true }
        if (G.shock) return
        if (G.shootme) each("shootem")
        if (G.shootme) return
        if (!G.live) {
            if (!G.live && G.compgo && !G.playerturn) {
                liver = false
                if (G.showgunright && !looky) { each("lookdown"); looky = true }
                if (G.compgo && !G.showgunright) each("scared")
                if (G.compgo && !G.showgunright && rand(100) == 1) each("scared2")
            }
        }
        if (G.live && !liver) { each("click"); liver = true }
        if (G.playerturn && rand(200) == 1) each("scared2")
        if (G.showgunright) { looky = false; liver = false }
    }

    override fun render(c: Canvas, p: Paint, tp: Paint) {
        blur.fade()
        var toBuf = false
        if (shaker > 0 || flasher) { blur.fade(); toBuf = true }
        if (G.deado2) { blur.fade(0.8f); toBuf = true }
        if (toBuf) { drawGfx(c, p, tp, blur.canvas); blur.touched() } else drawGfx(c, p, tp)
        blur.composite(c)
    }
}

// ------------------------------------------------------------ мир
class GameWorld : World() {
    private var tick = 0
    private var spinamount = 0
    private var startlevel = false
    private var firstscreen = true
    private var spinning = false
    private var ending = false
    private var ngtime = false

    private fun resetvar() {
        G.resetvar()
        tick = 0; spinamount = 0; startlevel = false
        firstscreen = true; spinning = false; ending = false
    }

    override fun update() {
        FP.camX = 0f
        FP.camY = -10f
        super.update()
        if (G.erase) {
            G.luckis = false
            removeAll()
            resetvar()
        }
        if (!G.goon && !ngtime) { add(Ngbig(0f, -8f)); ngtime = true }
        if (!G.goon) return
        if (G.theend && !ending) { add(Theend(-40f, -8f)); ending = true }
        if (G.theend) return
        if (firstscreen) {
            A.snd.amon.stop(); A.snd.music2.stop(); A.snd.music.stop()
            add(Sounder(660f, -5f))
            add(Firsty(0f, -8f))
            add(Ngtank(-5f, -8f))
            add(HUD())
            add(Heat(0f, -30f))
            firstscreen = false
        }
        if (G.limby) { G.revars(); spinning = true }
        if (G.waitforit) return
        if (G.expo) return
        if (spinning) {
            G.spintime = true
            spinamount = FP.rand(60) + 12
            G.spinner(spinamount)
            spinning = false
        }
        if (!startlevel) {
            loadLevel()
            startlevel = true
            G.spintime = true
            spinamount = FP.rand(60) + 12
            G.spinner(spinamount)
        }
    }

    private fun loadLevel() {
        add(Bgwall(-100f, 50f))
        add(Rind(0f, 570f))
        add(Table(-8f, 380f))
        add(Body1(60f, 225f))
        add(Body2(520f, 225f))
        add(Head1(10f, 50f))
        add(Head2(490f, 50f))
        add(Gun(215f, 398f))
        add(Bluearm(150f, 130f))
        add(Hand2(62f, 360f))
        add(Hand3(580f, 360f))
        add(Bluearm2(248f, 130f))
        add(Bgwall(60f, -20f))
        add(Spin(5f, 505f))
        add(Shoot(270f, 545f))
        add(Shootother(520f, 515f))
        add(Glow(400f, -50f))
        add(Sidewall(620f, -20f))
        add(Sidewall2(-30f, -20f))
        add(Fan(350f, -210f))
        add(Cylinder(217f, 50f))
        add(Deceased())
        add(Impact(-17f, -5f))
        add(Muzzle(0f, -10f))
        add(Ngtank2(0f, -9f))
        add(Poster(330f, 100f))
        add(Manta(150f, 50f))
    }
}
