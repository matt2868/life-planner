package io.github.matt2868.lifeplanner

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import org.json.JSONObject
import kotlin.math.ceil

/** One set of widget colours. */
class Pal(val bg: Int, val ink: Int, val ink2: Int, val line: Int, val acc: Int, val accInk: Int)

/**
 * The widget's colours, copied from the theme picked in the app. When the app follows the
 * system's light/dark setting, Android picks the matching set each time the widget is drawn.
 */
class WidgetLook(private val light: Pal, private val dark: Pal, private val night: Boolean) {

    private fun set(views: RemoteViews, id: Int, method: String, pick: (Pal) -> Int) {
        if (Build.VERSION.SDK_INT >= 31) views.setColorInt(id, method, pick(light), pick(dark))
        else views.setInt(id, method, pick(if (night) dark else light))
    }

    /** Colours an image (all widget images are drawn in white so they can take any colour). */
    fun tint(views: RemoteViews, id: Int, pick: (Pal) -> Int) = set(views, id, "setColorFilter", pick)

    fun text(views: RemoteViews, id: Int, pick: (Pal) -> Int) = set(views, id, "setTextColor", pick)

    companion object {
        private val FALLBACK_LIGHT = Pal(
            0xFFFFFFFF.toInt(), 0xFF15171C.toInt(), 0xFF5B606D.toInt(),
            0xFFE1E3EA.toInt(), 0xFF3037C4.toInt(), 0xFFFFFFFF.toInt()
        )
        private val FALLBACK_DARK = Pal(
            0xFF1C1F26.toInt(), 0xFFECEEF3.toInt(), 0xFF9DA3B0.toInt(),
            0xFF2B2F39.toInt(), 0xFF9196FF.toInt(), 0xFF0E1030.toInt()
        )

        fun load(context: Context): WidgetLook {
            val night = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
            val look = WidgetStore.look(context)
            var l = parse(look?.optJSONObject("light")) ?: FALLBACK_LIGHT
            var d = parse(look?.optJSONObject("dark")) ?: FALLBACK_DARK
            when (look?.optString("mode")) {
                "light" -> d = l
                "dark" -> l = d
            }
            return WidgetLook(l, d, night)
        }

        private fun parse(o: JSONObject?): Pal? {
            if (o == null) return null
            return try {
                Pal(
                    col(o, if (o.has("card")) "card" else "bg"), col(o, "ink"), col(o, "ink2"),
                    col(o, "line"), col(o, "acc"), col(o, "accInk")
                )
            } catch (e: Exception) {
                null
            }
        }

        private fun col(o: JSONObject, key: String): Int = Color.parseColor(o.getString(key))
    }
}

/** Draws the widget title in the app's heading font (widgets can't use custom fonts directly). */
object TitleArt {
    private var font: Typeface? = null

    fun bitmap(context: Context, text: String): Bitmap? = try {
        val tf = font ?: context.resources.getFont(R.font.fraunces_title).also { font = it }
        val dm = context.resources.displayMetrics
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            typeface = tf
            textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 22f, dm)
            color = Color.WHITE
        }
        val fm = paint.fontMetrics
        val w = ceil(paint.measureText(text)).toInt() + 2
        val h = ceil(fm.descent - fm.ascent).toInt()
        Bitmap.createBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1), Bitmap.Config.ARGB_8888).also {
            Canvas(it).drawText(text, 1f, -fm.ascent, paint)
        }
    } catch (e: Exception) {
        null
    }
}
