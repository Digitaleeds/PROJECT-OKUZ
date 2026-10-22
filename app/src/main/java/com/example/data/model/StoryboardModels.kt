package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

@IgnoreExtraProperties
data class StoryboardPage(
    @DocumentId val id: String = "",
    val userId: String = "",
    val pageNumber: Int = 1,
    val completed: Boolean = false,
    val ruleDorsal: Boolean = false,
    val ruleHood: Boolean = false,
    val ruleTree: Boolean = false,
    val ruleObject: Boolean = false,
    val mandate: String = "Unknown / TBD",
    val cameraDistance: String = "",
    val separationForce: String = "",
    val storyline: String = "",
    val illustration: String = "",
    val kidPosture: String = "",
    val dogAction: String = "",
    val distanceApart: String = "",
    val trigger: String = "",
    val response: String = "",
    val turningPoint: String = "",
    val terrain: String = "",
    val obstacle: String = "",
    val sketchStrokes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun activeRuleLabels(): List<String> = buildList {
        if (ruleDorsal) add("Viewed from behind / dorsal angle")
        if (ruleHood) add("Deep hooded jacket shadow")
        if (ruleTree) add("Eclipsed by tree / terrain")
        if (ruleObject) add("Blocked by misc. foreground object")
    }

    companion object {
        fun createBlank(pageNumber: Int, userId: String = ""): StoryboardPage {
            val clamped = pageNumber.coerceIn(1, 25)
            return StoryboardPage(
                id = "page_${clamped.toString().padStart(2, '0')}",
                userId = userId,
                pageNumber = clamped,
                completed = false,
                ruleDorsal = false,
                ruleHood = false,
                ruleTree = false,
                ruleObject = false,
                mandate = "Unknown / TBD"
            )
        }
    }
}

data class SketchPoint(
    val x: Float,
    val y: Float
)

data class SketchStroke(
    val id: String = UUID.randomUUID().toString(),
    val colorHex: String = "#F59E0B",
    val strokeWidth: Float = 3.5f,
    val isEraser: Boolean = false,
    val points: List<SketchPoint> = emptyList()
)

enum class StampType(val id: String, val label: String) {
    HOOD("hood", "+ Hood Silhouette"),
    CHIWEENIE("chiweenie", "+ Chiweenie Scale"),
    OBSTRUCTION("obstruction", "+ Foreground Obstruction");

    companion object {
        fun fromId(id: String): StampType = entries.firstOrNull { it.id == id } ?: HOOD
    }
}

data class SketchStamp(
    val id: String = UUID.randomUUID().toString(),
    val type: StampType = StampType.HOOD,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
)

data class SketchLayer(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Layer 1",
    val visible: Boolean = true,
    val opacity: Float = 1.0f,
    val strokes: List<SketchStroke> = emptyList(),
    val stamps: List<SketchStamp> = emptyList()
) {
    val elementCount: Int get() = strokes.size + stamps.size
}

data class SketchCanvasState(
    val layers: List<SketchLayer> = defaultLayers(),
    val activeLayerId: String = layers.lastOrNull()?.id ?: "",
    val isSpreadMode: Boolean = false,
    val combinedFacingPageNumber: Int? = null
) {
    val activeLayer: SketchLayer?
        get() = layers.firstOrNull { it.id == activeLayerId } ?: layers.lastOrNull()

    val hasAnyContent: Boolean
        get() = layers.any { it.strokes.isNotEmpty() || it.stamps.isNotEmpty() }

    companion object {
        fun defaultLayers(): List<SketchLayer> {
            val l1 = SketchLayer(id = "layer_env", name = "L1 // TERRAIN & OBSTRUCTION", visible = true)
            val l2 = SketchLayer(id = "layer_chars", name = "L2 // HOOD & CHIWEENIE", visible = true)
            val l3 = SketchLayer(id = "layer_fx", name = "L3 // CAMERA & NOTES", visible = true)
            return listOf(l1, l2, l3)
        }

        fun deserialize(raw: String): SketchCanvasState {
            if (raw.isBlank()) return SketchCanvasState()
            return try {
                val root = JSONObject(raw)
                val layersArr = root.optJSONArray("layers") ?: return SketchCanvasState()
                val parsedLayers = mutableListOf<SketchLayer>()
                for (i in 0 until layersArr.length()) {
                    val lObj = layersArr.optJSONObject(i) ?: continue
                    val layerId = lObj.optString("id", "layer_$i")
                    val layerName = lObj.optString("name", "Layer ${i + 1}")
                    val visible = lObj.optBoolean("visible", true)
                    val opacity = lObj.optDouble("opacity", 1.0).toFloat().coerceIn(0.05f, 1.0f)

                    val strokesArr = lObj.optJSONArray("strokes") ?: JSONArray()
                    val parsedStrokes = mutableListOf<SketchStroke>()
                    for (s in 0 until strokesArr.length()) {
                        val sObj = strokesArr.optJSONObject(s) ?: continue
                        val ptsArr = sObj.optJSONArray("pts") ?: JSONArray()
                        val pts = mutableListOf<SketchPoint>()
                        for (p in 0 until ptsArr.length()) {
                            val pt = ptsArr.optJSONArray(p) ?: continue
                            if (pt.length() >= 2) {
                                pts.add(
                                    SketchPoint(
                                        x = pt.optDouble(0, 0.0).toFloat(),
                                        y = pt.optDouble(1, 0.0).toFloat()
                                    )
                                )
                            }
                        }
                        if (pts.isNotEmpty()) {
                            parsedStrokes.add(
                                SketchStroke(
                                    id = sObj.optString("id", "s_$s"),
                                    colorHex = sObj.optString("c", "#F59E0B"),
                                    strokeWidth = sObj.optDouble("w", 3.5).toFloat(),
                                    isEraser = sObj.optBoolean("e", false),
                                    points = pts
                                )
                            )
                        }
                    }

                    val stampsArr = lObj.optJSONArray("stamps") ?: JSONArray()
                    val parsedStamps = mutableListOf<SketchStamp>()
                    for (m in 0 until stampsArr.length()) {
                        val mObj = stampsArr.optJSONObject(m) ?: continue
                        parsedStamps.add(
                            SketchStamp(
                                id = mObj.optString("id", "m_$m"),
                                type = StampType.fromId(mObj.optString("t", "hood")),
                                offsetX = mObj.optDouble("ox", 0.0).toFloat(),
                                offsetY = mObj.optDouble("oy", 0.0).toFloat()
                            )
                        )
                    }

                    parsedLayers.add(
                        SketchLayer(
                            id = layerId,
                            name = layerName,
                            visible = visible,
                            opacity = opacity,
                            strokes = parsedStrokes,
                            stamps = parsedStamps
                        )
                    )
                }
                if (parsedLayers.isEmpty()) {
                    SketchCanvasState()
                } else {
                    val savedActive = root.optString("activeLayerId", parsedLayers.last().id)
                    val validActive = if (parsedLayers.any { it.id == savedActive }) {
                        savedActive
                    } else {
                        parsedLayers.last().id
                    }
                    val isSpreadMode = root.optBoolean("spread", false)
                    val combinedPage = if (root.has("facingPage")) root.optInt("facingPage") else null
                    SketchCanvasState(
                        layers = parsedLayers,
                        activeLayerId = validActive,
                        isSpreadMode = isSpreadMode,
                        combinedFacingPageNumber = combinedPage
                    )
                }
            } catch (e: Exception) {
                SketchCanvasState()
            }
        }

        fun serialize(state: SketchCanvasState): String {
            val root = JSONObject()
            root.put("v", 3)
            root.put("activeLayerId", state.activeLayerId)
            root.put("spread", state.isSpreadMode)
            if (state.combinedFacingPageNumber != null) {
                root.put("facingPage", state.combinedFacingPageNumber)
            }
            val layersArr = JSONArray()
            for (layer in state.layers) {
                val lObj = JSONObject()
                lObj.put("id", layer.id)
                lObj.put("name", layer.name.take(40))
                lObj.put("visible", layer.visible)
                lObj.put("opacity", (layer.opacity * 100).toInt() / 100.0)

                val strokesArr = JSONArray()
                for (stroke in layer.strokes) {
                    val sObj = JSONObject()
                    sObj.put("id", stroke.id)
                    sObj.put("c", stroke.colorHex)
                    sObj.put("w", (stroke.strokeWidth * 10).toInt() / 10.0)
                    sObj.put("e", stroke.isEraser)
                    val ptsArr = JSONArray()
                    for (pt in stroke.points) {
                        val pArr = JSONArray()
                        pArr.put((pt.x * 10).toInt() / 10.0)
                        pArr.put((pt.y * 10).toInt() / 10.0)
                        ptsArr.put(pArr)
                    }
                    sObj.put("pts", ptsArr)
                    strokesArr.put(sObj)
                }
                lObj.put("strokes", strokesArr)

                val stampsArr = JSONArray()
                for (stamp in layer.stamps) {
                    val mObj = JSONObject()
                    mObj.put("id", stamp.id)
                    mObj.put("t", stamp.type.id)
                    mObj.put("ox", (stamp.offsetX * 10).toInt() / 10.0)
                    mObj.put("oy", (stamp.offsetY * 10).toInt() / 10.0)
                    stampsArr.put(mObj)
                }
                lObj.put("stamps", stampsArr)
                layersArr.put(lObj)
            }
            root.put("layers", layersArr)
            return root.toString()
        }
    }
}

class SketchHistoryStack(private val maxDepth: Int = 50) {
    private val undoDeque = ArrayDeque<SketchCanvasState>()
    private val redoDeque = ArrayDeque<SketchCanvasState>()

    val canUndo: Boolean
        get() = undoDeque.isNotEmpty()

    val canRedo: Boolean
        get() = redoDeque.isNotEmpty()

    val undoSize: Int
        get() = undoDeque.size

    val redoSize: Int
        get() = redoDeque.size

    fun canUndoWithFallback(currentState: SketchCanvasState): Boolean {
        if (undoDeque.isNotEmpty()) return true
        val active = currentState.activeLayer ?: return false
        return active.strokes.isNotEmpty() || active.stamps.isNotEmpty()
    }

    fun recordBeforeMutation(stateBefore: SketchCanvasState) {
        if (undoDeque.lastOrNull() == stateBefore) {
            redoDeque.clear()
            return
        }
        if (undoDeque.size >= maxDepth) {
            undoDeque.removeFirst()
        }
        undoDeque.addLast(stateBefore)
        redoDeque.clear()
    }

    fun undo(currentState: SketchCanvasState): SketchCanvasState? {
        if (undoDeque.isEmpty()) {
            // Fallback for pre-loaded strokes/stamps on active layer when session history is empty
            val targetLayer = currentState.activeLayer ?: return null
            if (targetLayer.strokes.isEmpty() && targetLayer.stamps.isEmpty()) return null
            val updatedLayers = currentState.layers.map { layer ->
                if (layer.id == targetLayer.id) {
                    when {
                        layer.strokes.isNotEmpty() -> layer.copy(strokes = layer.strokes.dropLast(1))
                        layer.stamps.isNotEmpty() -> layer.copy(stamps = layer.stamps.dropLast(1))
                        else -> layer
                    }
                } else {
                    layer
                }
            }
            if (redoDeque.size >= maxDepth) {
                redoDeque.removeFirst()
            }
            redoDeque.addLast(currentState)
            return currentState.copy(layers = updatedLayers)
        }
        val previous = undoDeque.removeLast()
        if (redoDeque.size >= maxDepth) {
            redoDeque.removeFirst()
        }
        redoDeque.addLast(currentState)
        return previous
    }

    fun redo(currentState: SketchCanvasState): SketchCanvasState? {
        if (redoDeque.isEmpty()) return null
        val next = redoDeque.removeLast()
        if (undoDeque.size >= maxDepth) {
            undoDeque.removeFirst()
        }
        undoDeque.addLast(currentState)
        return next
    }

    fun clear() {
        undoDeque.clear()
        redoDeque.clear()
    }
}


data class StoryPhase(
    val id: Int,
    val startPage: Int,
    val endPage: Int,
    val code: String,
    val title: String,
    val focus: String
)

data class EqBookPillar(
    val bookNumber: Int,
    val yearLabel: String,
    val pillarTitle: String,
    val subtitle: String,
    val isActiveBook: Boolean
)

val EQ_FIVE_YEAR_PILLARS = listOf(
    EqBookPillar(
        bookNumber = 1,
        yearLabel = "BOOK 1 • YEAR 1",
        pillarTitle = "1. Self-Regulation",
        subtitle = "Impulse control, steadiness under fire, & fighting to reunite across harsh terrain.",
        isActiveBook = true
    ),
    EqBookPillar(
        bookNumber = 2,
        yearLabel = "BOOK 2 • YEAR 2",
        pillarTitle = "2. Self-Awareness",
        subtitle = "Recognizing internal emotional tides, fears, and strengths in the wilderness.",
        isActiveBook = false
    ),
    EqBookPillar(
        bookNumber = 3,
        yearLabel = "BOOK 3 • YEAR 3",
        pillarTitle = "3. Internal Motivation",
        subtitle = "Sustaining drive and resilience when landmarks vanish and hope is tested.",
        isActiveBook = false
    ),
    EqBookPillar(
        bookNumber = 4,
        yearLabel = "BOOK 4 • YEAR 4",
        pillarTitle = "4. Empathy",
        subtitle = "Reading unspoken signals and shared vulnerability across living creatures.",
        isActiveBook = false
    ),
    EqBookPillar(
        bookNumber = 5,
        yearLabel = "BOOK 5 • YEAR 5",
        pillarTitle = "5. Social Skills",
        subtitle = "Leadership, cooperative trust, and guiding a community home.",
        isActiveBook = false
    )
)

val BOOK_ONE_PHASES = listOf(
    StoryPhase(
        id = 1,
        startPage = 1,
        endPage = 5,
        code = "Pages 01-05",
        title = "The Fragile Spark — Initial Shock & Impulse",
        focus = "First meeting between the hooded 10-year-old and the tiny pointy-eared Chiweenie amidst a looming natural threat."
    ),
    StoryPhase(
        id = 2,
        startPage = 6,
        endPage = 10,
        code = "Pages 06-10",
        title = "Torn Apart — Panic vs. Steadiness",
        focus = "Nature or bad luck forces the first sudden separation; the child must regulate fear to read tracks in harsh terrain."
    ),
    StoryPhase(
        id = 3,
        startPage = 11,
        endPage = 15,
        code = "Pages 11-15",
        title = "Against All Odds — Frustration & Adaptation",
        focus = "Near-misses and environmental obstacles test both kid and Chiweenie as fate repeatedly blocks their reunion."
    ),
    StoryPhase(
        id = 4,
        startPage = 16,
        endPage = 20,
        code = "Pages 16-20",
        title = "The Darkest Squall — Emotional Mastery Under Fire",
        focus = "Peak dramatic-cinematic crisis in the harshest environment; calm self-regulation replaces reckless desperation."
    ),
    StoryPhase(
        id = 5,
        startPage = 21,
        endPage = 25,
        code = "Pages 21-25",
        title = "Earned Reunion — Grounded Trust & Bond",
        focus = "The child and Chiweenie overcome the final barrier together, anchoring their lifeline for Book Two."
    )
)

fun getPhaseForPage(pageNumber: Int): StoryPhase {
    return BOOK_ONE_PHASES.firstOrNull { pageNumber in it.startPage..it.endPage }
        ?: BOOK_ONE_PHASES.first()
}

val CAMERA_DISTANCE_OPTIONS = listOf(
    "Extreme Wide",
    "Wide Env.",
    "Medium Action",
    "Close-Up (Obscured)"
)

val SEPARATION_FORCE_OPTIONS = listOf(
    "Nature",
    "Bad Luck",
    "Fate"
)
