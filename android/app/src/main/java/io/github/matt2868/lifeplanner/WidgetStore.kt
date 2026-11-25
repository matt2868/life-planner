package io.github.matt2868.lifeplanner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class Row(
    val type: Int,
    val text: String,
    val kind: String = "",
    val date: String = "",
    val id: String = "",
    val done: Boolean = false
)

/**
 * The widget's copy of the to-dos. The page writes a snapshot here; ticks made on the widget
 * update the snapshot straight away and are queued for the page to apply next time it opens.
 */
object WidgetStore {
    const val HEADER = 0
    const val ITEM = 1
    private const val PREFS = "widget"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun data(context: Context): JSONObject? =
        prefs(context).getString("data", null)?.let {
            try {
                JSONObject(it)
            } catch (e: Exception) {
                null
            }
        }

    @Synchronized
    fun setData(context: Context, json: String) {
        prefs(context).edit().putString("data", json).commit()
    }

    @Synchronized
    fun takeActions(context: Context): String {
        val p = prefs(context)
        val actions = p.getString("actions", "[]") ?: "[]"
        p.edit().putString("actions", "[]").commit()
        return actions
    }

    @Synchronized
    fun toggle(context: Context, kind: String, date: String, id: String) {
        val d = data(context) ?: return
        val arr = when (kind) {
            "gen" -> d.optJSONArray("general")
            "work" -> d.optJSONArray("work")
            else -> d.optJSONObject("days")?.optJSONArray(date)
        }
        if (arr == null) return
        var found = false
        var now = false
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            if (o.optString("id") == id) {
                now = !o.optBoolean("d")
                o.put("d", now)
                found = true
            }
        }
        if (!found) return
        val p = prefs(context)
        val actions = try {
            JSONArray(p.getString("actions", "[]"))
        } catch (e: Exception) {
            JSONArray()
        }
        actions.put(
            JSONObject().put("kind", kind).put("date", date).put("id", id).put("d", now)
                .put("at", System.currentTimeMillis())
        )
        p.edit().putString("data", d.toString()).putString("actions", actions.toString()).commit()
        if (now && kind == "day" && date == LocalDate.now().toString()) Reminders.markFilledToday(context)
    }

    fun look(context: Context): JSONObject? = data(context)?.optJSONObject("look")

    /** True until the planner has been set up (or the app has never been opened). */
    fun isSetup(context: Context): Boolean = data(context)?.optBoolean("setup") ?: true

    /** The small line under the widget title. */
    fun summary(context: Context, mode: String): String {
        val d = data(context)
        val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.UK))
        val ready = d != null && !d.optBoolean("setup")
        when (mode) {
            "work" -> return if (ready) "${open(d?.optJSONArray("work"))} open" else ""
            "gen" -> return if (ready) "${open(d?.optJSONArray("general"))} open" else ""
        }
        if (!ready) return date
        val today = d?.optJSONObject("days")?.optJSONArray(LocalDate.now().toString())
        val total = today?.length() ?: 0
        if (mode != "day" || total == 0) return date
        return "$date · ${total - open(today)} of $total done"
    }

    private fun open(arr: JSONArray?): Int {
        if (arr == null) return 0
        var n = 0
        for (i in 0 until arr.length()) if (arr.optJSONObject(i)?.optBoolean("d") == false) n++
        return n
    }

    /** Empty means nothing to show; the widget then shows its empty view. */
    fun rows(context: Context, mode: String = "all"): List<Row> {
        val d = data(context) ?: return emptyList()
        if (d.optBoolean("setup")) return emptyList()
        val today = LocalDate.now().toString()
        val todays = items(d.optJSONObject("days")?.optJSONArray(today), "day", today, keepDone = true)
        val work = items(d.optJSONArray("work"), "work", "", keepDone = false)
        val general = items(d.optJSONArray("general"), "gen", "", keepDone = false)
        when (mode) {
            "day" -> return todays
            "work" -> return work
            "gen" -> return general
        }
        val out = mutableListOf<Row>()
        out.addAll(todays)
        if (work.isNotEmpty()) {
            out.add(Row(HEADER, "Work"))
            out.addAll(work)
        }
        if (general.isNotEmpty()) {
            out.add(Row(HEADER, "General"))
            out.addAll(general)
        }
        return out
    }

    private fun items(arr: JSONArray?, kind: String, date: String, keepDone: Boolean): List<Row> {
        if (arr == null) return emptyList()
        val out = mutableListOf<Row>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val done = o.optBoolean("d")
            if (done && !keepDone) continue
            out.add(Row(ITEM, o.optString("x"), kind, date, o.optString("id"), done))
        }
        return out
    }
}
