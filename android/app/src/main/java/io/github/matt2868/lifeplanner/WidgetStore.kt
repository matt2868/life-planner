package io.github.matt2868.lifeplanner

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

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
        val arr = if (kind == "gen") d.optJSONArray("general") else d.optJSONObject("days")?.optJSONArray(date)
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
    }

    /** Empty means the planner hasn't been set up, so the widget shows its prompt instead. */
    fun rows(context: Context): List<Row> {
        val d = data(context) ?: return emptyList()
        if (d.optBoolean("setup")) return emptyList()
        val today = LocalDate.now().toString()
        val out = mutableListOf<Row>()
        out.add(Row(HEADER, "Today"))
        val todays = items(d.optJSONObject("days")?.optJSONArray(today), "day", today, keepDone = true)
        if (todays.isEmpty()) out.add(Row(HEADER, "Nothing planned. Tap + to add a to-do.")) else out.addAll(todays)
        val general = items(d.optJSONArray("general"), "gen", "", keepDone = false)
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
