package com.example.quizpirate.Utils

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import java.lang.Math.atan2
import java.lang.Math.hypot
import android.view.animation.LinearInterpolator

class ArcProgressBar @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {

    // ====== PROGRESSION GLOBALE ======
    var value = 0f
        set(v) {
            val newTarget = v.coerceAtLeast(0f)
            field = newTarget
            startSmoothAnimationTo(newTarget)     // <<< lance l'anim à chaque tick
        }

    var max = 100f
        set(v) { field = v.coerceAtLeast(0.0001f); invalidate() }

    // --- Progression "affichée" (lissée)
    private var displayValue = 0f

    /** Durée d'interpolation entre deux ticks. Mets 1000ms si ton timer ticke chaque seconde. */
    var animDurationMs: Long = 1100L

    /** Si true, on interpole; si false, affichage direct (utile pour debug). */
    var smoothEnabled: Boolean = true

    private var progressAnimator: ValueAnimator? = null

    // ====== STYLE DE L’ARC ======
    var strokeWidthPx = 24f
        set(v) { field = v; bgPaint.strokeWidth = v; fgPaint.strokeWidth = v; invalidate() }

    /** 0.0 = presque droit, 0.5+ = arc plus haut */
    var arcHeightRatio = 0.55f
    private var endTangentFactor = 0.28f

    private var bgColor = Color.parseColor("#2D3338")
        set(v) { field = v; bgPaint.color = v; invalidate() }

    private var fgColor = Color.parseColor("#00E5FF")
        set(v) { field = v; fgPaint.color = v; invalidate() }

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.BUTT
        color = bgColor
    }
    private val fgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = strokeWidthPx
        strokeCap = Paint.Cap.BUTT
        color = fgColor
    }

    private val fullPath = Path()
    private val segPath = Path()
    private val pm = PathMeasure()

    // ====== RATIO GLOBAL DES SPRITES ======
    var spriteGlobalRatio = 1.0f   // agit sur les 3 sprites
        set(v) { field = v.coerceAtLeast(0.05f); invalidate() }

    var scaleByStrokeWidth = true  // lie la taille des sprites à strokeWidthPx
        set(v) { field = v; invalidate() }

    private val baseStrokeRef = 24f

    // ====== OBJET 1 : VAISSEAU CIBLE (lead) ======
    private var shipBitmap: Bitmap? = null
    var shipScale = 0.6f
    var drawShip = true
    var shipAheadPx = 0f
    var alignToTangent = true

    fun setShipResource(resId: Int) {
        val d = ContextCompat.getDrawable(context, resId) ?: return
        shipBitmap = drawableToBitmap(d); invalidate()
    }

    // ====== OBJET 2 : VAISSEAU POURSUIVANT ======
    private var chaserBitmap: Bitmap? = null
    var chaserScale = 0.55f
    var drawChaser = true
    var chaserAheadPx = -2f
    var chaserAlignToTangent = true

    /** 0..1 : vitesse relative au vaisseau cible (doit être < torpedoSpeed < 1.0) */
    var chaserSpeed = 0.84f
    /** retard de départ (en pourcentage de l’arc) pour le garder derrière */
    var chaserLagPct = 0.12f

    fun setChaserResource(resId: Int) {
        val d = ContextCompat.getDrawable(context, resId) ?: return
        chaserBitmap = drawableToBitmap(d); invalidate()
    }

    // ====== OBJET 3 : TORPILLE ======
    private var torpedoBitmap: Bitmap? = null
    var torpedoScale = 0.40f
    var drawTorpedo = true
    var torpedoAheadPx = 6f
    var torpedoAlignToTangent = true

    /** Entre chaserSpeed et 1.0 (mais < 1.0 pour rester plus lente que la cible) */
    var torpedoSpeed = 0.92f
    /** lag pour rester derrière la cible, mais moins derrière que le chaser */
    var torpedoLagPct = 0.08f

    fun setTorpedoResource(resId: Int) {
        val d = ContextCompat.getDrawable(context, resId) ?: return
        torpedoBitmap = drawableToBitmap(d); invalidate()
    }

    // ====== SPAWN / APPARITION AVANT LA BARRE ======
    var torpedoEntryDistancePx = 80f
    var chaserEntryDistancePx  = 110f

    var torpedoAppearRangePct = 0.06f
    var chaserAppearRangePct  = 0.08f

    var torpedoMinScaleRatio = 0.6f
    var chaserMinScaleRatio  = 0.5f

    // ====== TEXTE CENTRE ======
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
    }
    var showTimeText: Boolean = true
    var formatTime: Boolean = true   // true = mm:ss, false = %

    // ====== UTILS ======
    private fun drawableToBitmap(d: Drawable): Bitmap {
        val bmp = if (d.intrinsicWidth > 0 && d.intrinsicHeight > 0)
            createBitmap(d.intrinsicWidth, d.intrinsicHeight)
        else
            createBitmap(64, 64)
        val c = Canvas(bmp)
        d.setBounds(0, 0, c.width, c.height)
        d.draw(c)
        return bmp
    }

    private fun effectiveScale(objectScale: Float): Float {
        var s = objectScale * spriteGlobalRatio
        if (scaleByStrokeWidth) {
            s *= (strokeWidthPx / baseStrokeRef)
        }
        return s
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
    private fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun startSmoothAnimationTo(target: Float) {
        if (!smoothEnabled) {
            displayValue = target
            invalidate()
            return
        }
        // Annule une éventuelle anim en cours pour repartir proprement
        progressAnimator?.cancel()

        val start = displayValue
        if (start == target) { invalidate(); return }

        progressAnimator = ValueAnimator.ofFloat(start, target).apply {
            duration = animDurationMs
            interpolator = LinearInterpolator()  // vitesse constante le long de l’arc
            addUpdateListener {
                displayValue = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        // --- géométrie de l’arc ---
        val left = paddingLeft + strokeWidthPx / 2f
        val right = width - paddingRight - strokeWidthPx / 2f
        val bottom = height - paddingBottom - strokeWidthPx / 2f
        val topSafe = paddingTop + strokeWidthPx / 2f

        val w = right - left
        val hAvail = bottom - topSafe
        val lift = (hAvail * arcHeightRatio).coerceAtLeast(0f)

        val c1x = left + w * endTangentFactor
        val c1y = bottom - lift
        val c2x = right - w * endTangentFactor
        val c2y = bottom - lift

        fullPath.reset()
        fullPath.moveTo(left, bottom)
        fullPath.cubicTo(c1x, c1y, c2x, c2y, right, bottom)

        // --- clip & dessins des arcs ---
        canvas.save()
        canvas.clipRect(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            (width - paddingRight).toFloat(),
            (height - paddingBottom).toFloat()
        )
        canvas.drawPath(fullPath, bgPaint)

        val pctLead = (displayValue / max).coerceIn(0f, 1f)  // <<< ici

        pm.setPath(fullPath, false)
        val len = pm.length
        segPath.reset()
        pm.getSegment(0f, len * pctLead, segPath, true)
        canvas.drawPath(segPath, fgPaint)
        canvas.restore()

        // --- positions dépendant des vitesses relatives ---
        // CIBLE (lead) : vitesse = 1.0
        val pLead = pctLead

        // POURSUIVANT : plus lent + plus de retard
        val pChaser = ((pctLead * chaserSpeed) - chaserLagPct).coerceIn(-1f, 1f)

        // TORPILLE : plus rapide que le poursuivant, mais plus lente que la cible
        val boundedTorpedoSpeed = torpedoSpeed.coerceIn(chaserSpeed + 0.001f, 0.999f)
        val pTorpedo = ((pctLead * boundedTorpedoSpeed) - torpedoLagPct).coerceIn(-1f, 1f)

        // --- dessins des objets, hors clip ---
        pm.setPath(fullPath, false)

        if (drawTorpedo && torpedoBitmap != null) {
            drawBitmapWithSpawn(
                canvas, pm, len, pTorpedo, torpedoBitmap!!, effectiveScale(torpedoScale),
                torpedoAheadPx, torpedoAlignToTangent,
                appearRangePct = torpedoAppearRangePct,
                entryDistancePx = torpedoEntryDistancePx,
                minScaleRatio = torpedoMinScaleRatio
            )
        }
        // Ordre d’affichage : chaser -> torpedo -> lead (pour que la cible reste « au-dessus »)
        if (drawChaser && chaserBitmap != null) {
            drawBitmapWithSpawn(
                canvas, pm, len, pChaser, chaserBitmap!!, effectiveScale(chaserScale),
                chaserAheadPx, chaserAlignToTangent,
                appearRangePct = chaserAppearRangePct,
                entryDistancePx = chaserEntryDistancePx,
                minScaleRatio = chaserMinScaleRatio
            )
        }

        if (drawShip && shipBitmap != null) {
            drawBitmapOnPath(
                canvas, pm, len, pLead, shipBitmap!!,
                effectiveScale(shipScale), shipAheadPx, alignToTangent
            )
        }

        // --- texte central ---
        if (showTimeText) {
            val centerX = width / 2f
            val centerY = height / 2f - 40f
            val text = if (formatTime) {
                val remaining = ((max - value).toLong() / 1000).coerceAtLeast(0)
                val minutes = remaining / 60
                val seconds = remaining % 60
                String.format("%d:%02d", minutes, seconds)
            } else {
                "${(pctLead * 100).toInt()} %"
            }
            val fm = textPaint.fontMetrics
            val textY = centerY - (fm.ascent + fm.descent) / 2
            canvas.drawText(text, centerX, textY, textPaint)
        }
    }

    // ====== DESSINS ======

    private fun drawBitmapOnPath(
        canvas: Canvas,
        pathMeasure: PathMeasure,
        length: Float,
        pct: Float,
        bmp: Bitmap,
        scale: Float,
        aheadPx: Float,
        align: Boolean
    ) {
        val clamped = pct.coerceIn(0f, 1f)
        val target = (length * clamped).coerceIn(0f, length)

        val pos = FloatArray(2)
        val tan = FloatArray(2)
        pathMeasure.getPosTan(target, pos, tan)

        if (aheadPx != 0f) {
            val norm = hypot(tan[0].toDouble(), tan[1].toDouble()).toFloat().coerceAtLeast(1e-6f)
            pos[0] += (tan[0] / norm) * aheadPx
            pos[1] += (tan[1] / norm) * aheadPx
        }

        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()

        val m = Matrix()
        val s = scale.coerceIn(0.05f, 3f)
        m.postScale(s, s)

        if (align) {
            val angleDeg = Math.toDegrees(atan2(tan[1].toDouble(), tan[0].toDouble())).toFloat()
            m.postRotate(angleDeg, bw * s / 2f, bh * s / 2f)
        }

        m.postTranslate(pos[0] - (bw * s / 2f), pos[1] - (bh * s / 2f))
        canvas.drawBitmap(bmp, m, null)
    }

    /**
     * Dessine un bitmap avec "apparition" quand pctObj < 0, puis suit le path quand pctObj >= 0.
     *
     * @param pctObj           progression propre à l'objet (ex: pChaser, pTorpedo), dans [-1..1].
     * @param appearRangePct   largeur de la fenêtre d'apparition avant 0 (ex: 0.06f).
     * @param entryDistancePx  distance avant le début de l'arc (en px) d'où l'objet arrive.
     * @param minScaleRatio    ratio de scale minimal au début de l'apparition (→ 1.0).
     */
    private fun drawBitmapWithSpawn(
        canvas: Canvas,
        pathMeasure: PathMeasure,
        length: Float,
        pctObj: Float,
        bmp: Bitmap,
        baseScale: Float,
        aheadPx: Float,
        align: Boolean,
        appearRangePct: Float,
        entryDistancePx: Float,
        minScaleRatio: Float
    ) {
        val p = pctObj.coerceIn(-1f, 1f)

        // Début d'arc (pos0, tan0)
        val startPos = FloatArray(2)
        val startTan = FloatArray(2)
        pathMeasure.getPosTan(0f, startPos, startTan)

        val norm0 = hypot(startTan[0].toDouble(), startTan[1].toDouble()).toFloat().coerceAtLeast(1e-6f)
        val dir0x = startTan[0] / norm0
        val dir0y = startTan[1] / norm0

        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        val m = Matrix()

        val pos = FloatArray(2)
        val tan = FloatArray(2)

        var alpha = 255
        var scale = baseScale.coerceIn(0.05f, 3f)

        if (p < 0f) {
            // Phase d'apparition: p ∈ [-appearRangePct, 0] → fade/scale-in
            if (p >= -appearRangePct) {
                val t = smoothstep(-appearRangePct, 0f, p) // 0..1
                alpha = (255f * t).toInt().coerceIn(0, 255)
                val scaleRatio = lerp(minScaleRatio, 1f, t)
                scale = (baseScale * scaleRatio).coerceIn(0.05f, 3f)

                val back = entryDistancePx * (1f - t)
                pos[0] = startPos[0] - dir0x * back
                pos[1] = startPos[1] - dir0y * back

                tan[0] = startTan[0]
                tan[1] = startTan[1]
            } else {
                // Trop tôt → invisible
                return
            }
        } else {
            // Sur la barre : position normale
            val target = (length * p).coerceIn(0f, length)
            pathMeasure.getPosTan(target, pos, tan)

            if (aheadPx != 0f) {
                val norm = hypot(tan[0].toDouble(), tan[1].toDouble()).toFloat().coerceAtLeast(1e-6f)
                pos[0] += (tan[0] / norm) * aheadPx
                pos[1] += (tan[1] / norm) * aheadPx
            }
        }

        m.postScale(scale, scale)

        if (align) {
            val angleDeg = Math.toDegrees(atan2(tan[1].toDouble(), tan[0].toDouble())).toFloat()
            m.postRotate(angleDeg, bw * scale / 2f, bh * scale / 2f)
        }

        m.postTranslate(pos[0] - (bw * scale / 2f), pos[1] - (bh * scale / 2f))

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.alpha = alpha }
        canvas.drawBitmap(bmp, m, paint)
    }
}
