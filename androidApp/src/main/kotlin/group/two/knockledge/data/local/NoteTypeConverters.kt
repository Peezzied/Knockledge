package group.two.knockledge.data.local

import androidx.room.TypeConverter
import group.two.knockledge.data.model.ChapterMarker
import group.two.knockledge.data.model.Sentence
import org.json.JSONArray
import org.json.JSONObject

class NoteTypeConverters {
    @TypeConverter
    fun sentencesToJson(list: List<Sentence>?): String? = list?.let { l ->
        JSONArray(l.map {
            JSONObject()
                .put("speaker", it.speaker)
                .put("startTimeMs", it.startTimeMs)
                .put("endTimeMs", it.endTimeMs)
                .put("text", it.text)
                .put("confidence", it.confidence.toDouble())
        }).toString()
    }

    @TypeConverter
    fun jsonToSentences(json: String?): List<Sentence>? = json?.let { s ->
        val a = JSONArray(s)
        List(a.length()) {
            val o = a.getJSONObject(it)
            Sentence(
                o.getString("speaker"),
                o.getLong("startTimeMs"),
                o.getLong("endTimeMs"),
                o.getString("text"),
                o.getDouble("confidence").toFloat(),
            )
        }
    }

    @TypeConverter
    fun markersToJson(list: List<ChapterMarker>?): String? = list?.let { l ->
        JSONArray(l.map {
            JSONObject()
                .put("startTimeMs", it.startTimeMs)
                .put("title", it.title)
                .put("keyBullets", JSONArray(it.keyBullets))
        }).toString()
    }

    @TypeConverter
    fun jsonToMarkers(json: String?): List<ChapterMarker>? = json?.let { s ->
        val a = JSONArray(s)
        List(a.length()) {
            val o = a.getJSONObject(it)
            ChapterMarker(o.getLong("startTimeMs"), o.getString("title"), strings(o.getJSONArray("keyBullets")))
        }
    }

    @TypeConverter
    fun stringsToJson(list: List<String>?): String? = list?.let { JSONArray(it).toString() }

    @TypeConverter
    fun jsonToStrings(json: String?): List<String>? = json?.let { strings(JSONArray(it)) }

    private fun strings(a: JSONArray) = List(a.length()) { a.getString(it) }
}
