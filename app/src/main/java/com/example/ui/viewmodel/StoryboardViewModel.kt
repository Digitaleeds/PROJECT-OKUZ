package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.SketchCanvasState
import com.example.data.model.SketchHistoryStack
import com.example.data.model.SketchLayer
import com.example.data.model.SketchPoint
import com.example.data.model.SketchStamp
import com.example.data.model.SketchStroke
import com.example.data.model.StampType
import com.example.data.model.StoryboardPage
import com.example.data.repository.StoryboardRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

enum class StudioTab(val label: String, val code: String) {
    WORKSHEET("Storyboard Worksheet", "F1 // WORKSHEET"),
    TRACKER("25-Page Tracker", "F2 // TRACKER"),
    BLUEPRINT("Master Blueprint", "F3 // BLUEPRINT"),
    GALLERY("25-Page Overview", "F4 // OVERVIEW")
}

enum class DrawTool {
    PEN,
    ERASER
}

enum class SyncStatus(val badgeText: String) {
    AUTO_SAVED("AUTO-SAVED"),
    SAVING("SYNCING..."),
    SAVED("SAVED"),
    ERROR("SYNC FAULT")
}

enum class PageField {
    MANDATE,
    STORYLINE,
    ILLUSTRATION,
    KID_POSTURE,
    DOG_ACTION,
    DISTANCE_APART,
    TRIGGER,
    RESPONSE,
    TURNING_POINT,
    TERRAIN,
    OBSTACLE
}

class StoryboardViewModel(
    private val repository: StoryboardRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _pagesMap = MutableStateFlow<Map<Int, StoryboardPage>>(
        (1..25).associateWith { p -> StoryboardPage.createBlank(p, currentUserId) }
    )
    val pagesMap: StateFlow<Map<Int, StoryboardPage>> = _pagesMap.asStateFlow()

    private val _currentPageNumber = MutableStateFlow(1)
    val currentPageNumber: StateFlow<Int> = _currentPageNumber.asStateFlow()

    private val _activeTab = MutableStateFlow(StudioTab.WORKSHEET)
    val activeTab: StateFlow<StudioTab> = _activeTab.asStateFlow()

    private val _syncStatus = MutableStateFlow(SyncStatus.AUTO_SAVED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    // Interactive Multi-Layer Sketchpad state for the currently selected page
    private val _sketchState = MutableStateFlow(SketchCanvasState())
    val sketchState: StateFlow<SketchCanvasState> = _sketchState.asStateFlow()

    private val _drawTool = MutableStateFlow(DrawTool.PEN)
    val drawTool: StateFlow<DrawTool> = _drawTool.asStateFlow()

    private val _penColorHex = MutableStateFlow("#F59E0B")
    val penColorHex: StateFlow<String> = _penColorHex.asStateFlow()

    private val _penStrokeWidth = MutableStateFlow(3.5f)
    val penStrokeWidth: StateFlow<Float> = _penStrokeWidth.asStateFlow()

    // Per-page Undo/Redo History Stacks (1..25)
    private val pageHistoryStacks = mutableMapOf<Int, SketchHistoryStack>()

    private val _canUndoSketch = MutableStateFlow(false)
    val canUndoSketch: StateFlow<Boolean> = _canUndoSketch.asStateFlow()

    private val _canRedoSketch = MutableStateFlow(false)
    val canRedoSketch: StateFlow<Boolean> = _canRedoSketch.asStateFlow()

    private val _undoHistoryCount = MutableStateFlow(0)
    val undoHistoryCount: StateFlow<Int> = _undoHistoryCount.asStateFlow()

    private val _redoHistoryCount = MutableStateFlow(0)
    val redoHistoryCount: StateFlow<Int> = _redoHistoryCount.asStateFlow()

    private val saveJobs = mutableMapOf<Int, Job>()

    private fun currentHistoryStack(): SketchHistoryStack {
        return pageHistoryStacks.getOrPut(_currentPageNumber.value) { SketchHistoryStack() }
    }

    private fun refreshSketchHistoryTelemetry() {
        val stack = currentHistoryStack()
        val current = _sketchState.value
        _canUndoSketch.value = stack.canUndoWithFallback(current)
        _canRedoSketch.value = stack.canRedo
        _undoHistoryCount.value = stack.undoSize
        _redoHistoryCount.value = stack.redoSize
    }

    init {
        observeCloudPages()
    }

    private fun observeCloudPages() {
        viewModelScope.launch {
            repository.observeUserPages(currentUserId)
                .catch { e ->
                    Log.w(TAG, "Error observing storyboard pages", e)
                    _syncStatus.value = SyncStatus.ERROR
                    _errorBanner.value = e.localizedMessage ?: "Failed to sync storyboard pages"
                }
                .collect { remotePages ->
                    val currentMap = _pagesMap.value.toMutableMap()
                    val remoteByNum = remotePages.associateBy { it.pageNumber }
                    for (p in 1..25) {
                        val remote = remoteByNum[p]
                        if (remote != null) {
                            // Only overwrite if there is no pending local debounce job for this page
                            if (saveJobs[p]?.isActive != true) {
                                currentMap[p] = remote
                            } else {
                                // Keep createdAt from remote so subsequent saves perform an UPDATE
                                val localPage = currentMap[p]
                                if (localPage != null && localPage.createdAt == null && remote.createdAt != null) {
                                    currentMap[p] = localPage.copy(createdAt = remote.createdAt)
                                }
                            }
                        }
                    }
                    _pagesMap.value = currentMap
                    // Refresh sketchState if the active page has no pending save job
                    val activePage = _currentPageNumber.value
                    if (saveJobs[activePage]?.isActive != true) {
                        val pageObj = currentMap[activePage]
                        if (pageObj != null) {
                            val currentActiveLayerId = _sketchState.value.activeLayerId
                            val loaded = SketchCanvasState.deserialize(pageObj.sketchStrokes)
                            val preservedActive = if (loaded.layers.any { it.id == currentActiveLayerId }) {
                                currentActiveLayerId
                            } else {
                                loaded.activeLayerId
                            }
                            _sketchState.value = loaded.copy(activeLayerId = preservedActive)
                            refreshSketchHistoryTelemetry()
                        }
                    }
                }
        }
    }

    fun switchTab(tab: StudioTab) {
        _activeTab.value = tab
    }

    fun selectPage(pageNumber: Int, switchToWorksheet: Boolean = true) {
        val clamped = pageNumber.coerceIn(1, 25)
        _currentPageNumber.value = clamped
        val pageData = _pagesMap.value[clamped] ?: StoryboardPage.createBlank(clamped, currentUserId)
        _sketchState.value = SketchCanvasState.deserialize(pageData.sketchStrokes)
        refreshSketchHistoryTelemetry()
        if (switchToWorksheet) {
            _activeTab.value = StudioTab.WORKSHEET
        }
    }

    fun prevPage() {
        val cur = _currentPageNumber.value
        if (cur > 1) selectPage(cur - 1, switchToWorksheet = false)
    }

    fun nextPage() {
        val cur = _currentPageNumber.value
        if (cur < 25) selectPage(cur + 1, switchToWorksheet = false)
    }

    fun setDrawTool(tool: DrawTool) {
        _drawTool.value = tool
    }

    fun setPenColor(hex: String) {
        _penColorHex.value = hex
        _drawTool.value = DrawTool.PEN
    }

    fun setPenStrokeWidth(width: Float) {
        _penStrokeWidth.value = width.coerceIn(1.5f, 16f)
    }

    // --- MULTI-LAYER SKETCHPAD OPERATIONS ---

    fun selectLayer(layerId: String) {
        val current = _sketchState.value
        if (current.layers.any { it.id == layerId }) {
            val updated = current.copy(activeLayerId = layerId)
            _sketchState.value = updated
            refreshSketchHistoryTelemetry()
        }
    }

    fun addLayer(customName: String? = null) {
        val current = _sketchState.value
        if (current.layers.size >= 10) return // Practical cap for CRT deck layers
        val nextNum = current.layers.size + 1
        val newLayer = SketchLayer(
            id = "layer_${UUID.randomUUID().toString().take(8)}",
            name = customName?.takeIf { it.isNotBlank() } ?: "L$nextNum // OVERLAY PASS",
            visible = true
        )
        val newLayers = current.layers + newLayer
        val updated = current.copy(layers = newLayers, activeLayerId = newLayer.id)
        commitSketchState(updated)
    }

    fun deleteLayer(layerId: String) {
        val current = _sketchState.value
        if (current.layers.size <= 1) {
            // If deleting the only remaining layer, clear its contents so 1 layer always exists
            val clearedOnly = current.layers.first().copy(strokes = emptyList(), stamps = emptyList())
            val updated = current.copy(layers = listOf(clearedOnly), activeLayerId = clearedOnly.id)
            commitSketchState(updated)
            return
        }
        val remaining = current.layers.filterNot { it.id == layerId }
        val nextActiveId = if (current.activeLayerId == layerId) {
            remaining.last().id
        } else {
            current.activeLayerId
        }
        val updated = current.copy(layers = remaining, activeLayerId = nextActiveId)
        commitSketchState(updated)
    }

    fun moveLayerUp(layerId: String) {
        // In our layer panel, top of list or higher index: let's support moving towards the top of stack (higher index) or swapping with adjacent
        val current = _sketchState.value
        val idx = current.layers.indexOfFirst { it.id == layerId }
        if (idx <= 0) return
        val mutable = current.layers.toMutableList()
        val temp = mutable[idx - 1]
        mutable[idx - 1] = mutable[idx]
        mutable[idx] = temp
        commitSketchState(current.copy(layers = mutable))
    }

    fun moveLayerDown(layerId: String) {
        val current = _sketchState.value
        val idx = current.layers.indexOfFirst { it.id == layerId }
        if (idx < 0 || idx >= current.layers.lastIndex) return
        val mutable = current.layers.toMutableList()
        val temp = mutable[idx + 1]
        mutable[idx + 1] = mutable[idx]
        mutable[idx] = temp
        commitSketchState(current.copy(layers = mutable))
    }

    fun toggleLayerVisibility(layerId: String) {
        val current = _sketchState.value
        val updatedLayers = current.layers.map { layer ->
            if (layer.id == layerId) layer.copy(visible = !layer.visible) else layer
        }
        commitSketchState(current.copy(layers = updatedLayers))
    }

    fun renameLayer(layerId: String, newName: String) {
        val trimmed = newName.trim().take(40)
        if (trimmed.isBlank()) return
        val current = _sketchState.value
        val updatedLayers = current.layers.map { layer ->
            if (layer.id == layerId) layer.copy(name = trimmed) else layer
        }
        commitSketchState(current.copy(layers = updatedLayers))
    }

    fun setLayerOpacity(layerId: String, opacity: Float) {
        val clamped = opacity.coerceIn(0.1f, 1.0f)
        val current = _sketchState.value
        val updatedLayers = current.layers.map { layer ->
            if (layer.id == layerId) layer.copy(opacity = clamped) else layer
        }
        commitSketchState(current.copy(layers = updatedLayers))
    }

    fun mergeLayerDown(layerId: String) {
        val current = _sketchState.value
        val idx = current.layers.indexOfFirst { it.id == layerId }
        // Merge with the layer below it (index + 1)
        if (idx < 0 || idx >= current.layers.lastIndex) return
        val upperLayer = current.layers[idx]
        val lowerLayer = current.layers[idx + 1]

        val combinedLower = lowerLayer.copy(
            strokes = lowerLayer.strokes + upperLayer.strokes,
            stamps = lowerLayer.stamps + upperLayer.stamps
        )
        val newLayers = current.layers.toMutableList()
        newLayers.removeAt(idx) // Remove upper layer
        newLayers[idx] = combinedLower // Replace lower layer with merged

        val nextActive = if (current.activeLayerId == upperLayer.id) combinedLower.id else current.activeLayerId
        val updated = current.copy(layers = newLayers, activeLayerId = nextActive)
        commitSketchState(updated)
    }

    fun flattenAllLayers() {
        val current = _sketchState.value
        if (current.layers.size <= 1) return
        val allStrokes = current.layers.flatMap { it.strokes }
        val allStamps = current.layers.flatMap { it.stamps }
        val singleLayer = SketchLayer(
            id = "layer_flattened_${UUID.randomUUID().toString().take(6)}",
            name = "L1 // FLATTENED COMPOSITE",
            visible = true,
            strokes = allStrokes,
            stamps = allStamps
        )
        val updated = current.copy(layers = listOf(singleLayer), activeLayerId = singleLayer.id)
        commitSketchState(updated)
    }

    fun toggleTwoPageSpread(enable: Boolean? = null, facingPageNumber: Int? = null) {
        val current = _sketchState.value
        val curPage = _currentPageNumber.value
        val nextSpreadMode = enable ?: !current.isSpreadMode
        val facing = facingPageNumber ?: current.combinedFacingPageNumber ?: run {
            if (curPage % 2 == 0) (curPage + 1).coerceAtMost(25) else (curPage - 1).coerceAtLeast(1)
        }
        val updated = current.copy(
            isSpreadMode = nextSpreadMode,
            combinedFacingPageNumber = if (nextSpreadMode) facing else null
        )
        commitSketchState(updated)
    }

    fun combineTwoPagesAsOneFull(
        facingPageNumber: Int,
        mergeManuscript: Boolean = true,
        onComplete: (String) -> Unit = {}
    ) {
        val curPageNum = _currentPageNumber.value
        val facingPageObj = _pagesMap.value[facingPageNumber]
        val curPageObj = _pagesMap.value[curPageNum] ?: StoryboardPage.createBlank(curPageNum, currentUserId)

        // 1. Extract facing page sketch strokes
        val facingSketch = if (facingPageObj != null) {
            SketchCanvasState.deserialize(facingPageObj.sketchStrokes)
        } else {
            SketchCanvasState()
        }

        // 2. Import facing page layers into current sketchpad
        val current = _sketchState.value
        val importedLayers = facingSketch.layers.filter { it.elementCount > 0 }.map { facingL ->
            SketchLayer(
                id = "spread_p${facingPageNumber}_${UUID.randomUUID().toString().take(6)}",
                name = "P${facingPageNumber.toString().padStart(2, '0')} // ${facingL.name}",
                visible = true,
                strokes = facingL.strokes,
                stamps = facingL.stamps
            )
        }

        val combinedLayers = if (importedLayers.isNotEmpty()) {
            current.layers + importedLayers
        } else {
            // If facing page had no strokes, create a placeholder layer for drawing the facing spread half
            current.layers + SketchLayer(
                id = "spread_p${facingPageNumber}_pass",
                name = "P${facingPageNumber.toString().padStart(2, '0')} // FACING SPREAD PASS",
                visible = true
            )
        }

        val updatedSketch = current.copy(
            layers = combinedLayers,
            activeLayerId = combinedLayers.last().id,
            isSpreadMode = true,
            combinedFacingPageNumber = facingPageNumber
        )
        currentHistoryStack().recordBeforeMutation(current)
        _sketchState.value = updatedSketch
        refreshSketchHistoryTelemetry()
        val serializedSketch = SketchCanvasState.serialize(updatedSketch)

        // 3. Optionally merge manuscript storyline and illustration descriptions
        var mergedStoryline = curPageObj.storyline
        var mergedIllustration = curPageObj.illustration
        if (mergeManuscript && facingPageObj != null) {
            if (facingPageObj.storyline.isNotBlank() && !mergedStoryline.contains(facingPageObj.storyline)) {
                mergedStoryline = if (mergedStoryline.isBlank()) {
                    facingPageObj.storyline
                } else {
                    "$mergedStoryline\n\n[PAGE ${facingPageNumber.toString().padStart(2, '0')} SPREAD CONTINUATION]:\n${facingPageObj.storyline}"
                }
            }
            if (facingPageObj.illustration.isNotBlank() && !mergedIllustration.contains(facingPageObj.illustration)) {
                mergedIllustration = if (mergedIllustration.isBlank()) {
                    facingPageObj.illustration
                } else {
                    "$mergedIllustration\n\n[PAGE ${facingPageNumber.toString().padStart(2, '0')} VISUALS]:\n${facingPageObj.illustration}"
                }
            }
        }

        val updatedPage = curPageObj.copy(
            sketchStrokes = serializedSketch,
            storyline = mergedStoryline,
            illustration = mergedIllustration
        )
        val mapCopy = _pagesMap.value.toMutableMap()
        mapCopy[curPageNum] = updatedPage
        _pagesMap.value = mapCopy
        scheduleSavePage(updatedPage)

        onComplete("Combined Page ${curPageNum.toString().padStart(2, '0')} and Page ${facingPageNumber.toString().padStart(2, '0')} into One Continuous Two-Page Spread!")
    }

    fun addStrokeToActiveLayer(points: List<SketchPoint>) {
        if (points.isEmpty()) return
        val current = _sketchState.value
        val targetLayer = current.activeLayer ?: return
        if (!targetLayer.visible) return // Do not draw on a hidden layer

        val isEraser = _drawTool.value == DrawTool.ERASER
        val updatedLayers = current.layers.map { layer ->
            if (layer.id == targetLayer.id) {
                if (isEraser) {
                    // Eraser removes strokes or stamps near the erased path on the active layer, OR adds an eraser path
                    val remainingStrokes = layer.strokes.filterNot { stroke ->
                        stroke.points.any { pt ->
                            points.any { ePt ->
                                val dx = pt.x - ePt.x
                                val dy = pt.y - ePt.y
                                (dx * dx + dy * dy) < 900f // 30px radius in 800x450 canvas space
                            }
                        }
                    }
                    layer.copy(strokes = remainingStrokes)
                } else {
                    val newStroke = SketchStroke(
                        colorHex = _penColorHex.value,
                        strokeWidth = _penStrokeWidth.value,
                        isEraser = false,
                        points = points
                    )
                    layer.copy(strokes = layer.strokes + newStroke)
                }
            } else {
                layer
            }
        }
        commitSketchState(current.copy(layers = updatedLayers))
    }

    fun stampMarkerOnActiveLayer(type: StampType) {
        val current = _sketchState.value
        val targetLayer = current.activeLayer ?: return
        val existingCount = targetLayer.stamps.count { it.type == type }
        val offsetStep = (existingCount % 4) * 18f
        val newStamp = SketchStamp(
            type = type,
            offsetX = offsetStep,
            offsetY = offsetStep * 0.6f
        )
        val updatedLayers = current.layers.map { layer ->
            if (layer.id == targetLayer.id) {
                layer.copy(visible = true, stamps = layer.stamps + newStamp)
            } else {
                layer
            }
        }
        commitSketchState(current.copy(layers = updatedLayers))
    }

    fun undoOnActiveLayer() {
        val current = _sketchState.value
        val previous = currentHistoryStack().undo(current) ?: return
        commitSketchState(previous, recordHistory = false)
    }

    fun redoOnActiveLayer() {
        val current = _sketchState.value
        val next = currentHistoryStack().redo(current) ?: return
        commitSketchState(next, recordHistory = false)
    }

    fun clearActiveLayer() {
        val current = _sketchState.value
        val targetLayer = current.activeLayer ?: return
        if (targetLayer.strokes.isEmpty() && targetLayer.stamps.isEmpty()) return
        val updatedLayers = current.layers.map { layer ->
            if (layer.id == targetLayer.id) {
                layer.copy(strokes = emptyList(), stamps = emptyList())
            } else {
                layer
            }
        }
        commitSketchState(current.copy(layers = updatedLayers))
    }

    fun clearAllSketchLayers() {
        val fresh = SketchCanvasState()
        commitSketchState(fresh)
    }

    private fun commitSketchState(newState: SketchCanvasState, recordHistory: Boolean = true) {
        val previousState = _sketchState.value
        if (recordHistory && previousState != newState) {
            currentHistoryStack().recordBeforeMutation(previousState)
        }
        _sketchState.value = newState
        refreshSketchHistoryTelemetry()
        val serialized = SketchCanvasState.serialize(newState)
        mutateCurrentPage { it.copy(sketchStrokes = serialized) }
    }

    // --- WORKSHEET FIELD UPDATES ---

    fun togglePageComplete() {
        mutateCurrentPage { it.copy(completed = !it.completed) }
    }

    fun updateRuleCheck(ruleKey: String, checked: Boolean) {
        mutateCurrentPage { page ->
            when (ruleKey) {
                "dorsal" -> page.copy(ruleDorsal = checked)
                "hood" -> page.copy(ruleHood = checked)
                "tree" -> page.copy(ruleTree = checked)
                "object" -> page.copy(ruleObject = checked)
                else -> page
            }
        }
    }

    fun setCameraDistance(distance: String) {
        mutateCurrentPage { page ->
            val next = if (page.cameraDistance == distance) "" else distance
            page.copy(cameraDistance = next)
        }
    }

    fun setSeparationForce(force: String) {
        mutateCurrentPage { page ->
            val next = if (page.separationForce == force) "" else force
            page.copy(separationForce = next)
        }
    }

    fun updateField(field: PageField, value: String) {
        mutateCurrentPage { page ->
            when (field) {
                PageField.MANDATE -> page.copy(mandate = value)
                PageField.STORYLINE -> page.copy(storyline = value)
                PageField.ILLUSTRATION -> page.copy(illustration = value)
                PageField.KID_POSTURE -> page.copy(kidPosture = value)
                PageField.DOG_ACTION -> page.copy(dogAction = value)
                PageField.DISTANCE_APART -> page.copy(distanceApart = value)
                PageField.TRIGGER -> page.copy(trigger = value)
                PageField.RESPONSE -> page.copy(response = value)
                PageField.TURNING_POINT -> page.copy(turningPoint = value)
                PageField.TERRAIN -> page.copy(terrain = value)
                PageField.OBSTACLE -> page.copy(obstacle = value)
            }
        }
    }

    fun resetCurrentPage() {
        val pageNum = _currentPageNumber.value
        saveJobs[pageNum]?.cancel()
        val blank = StoryboardPage.createBlank(pageNum, currentUserId)
        val updatedMap = _pagesMap.value.toMutableMap()
        updatedMap[pageNum] = blank
        _pagesMap.value = updatedMap
        currentHistoryStack().clear()
        _sketchState.value = SketchCanvasState()
        refreshSketchHistoryTelemetry()

        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SAVING
            val res = repository.deletePage(pageNum)
            if (res.isSuccess) {
                _syncStatus.value = SyncStatus.SAVED
                delay(650)
                _syncStatus.value = SyncStatus.AUTO_SAVED
            } else {
                _syncStatus.value = SyncStatus.ERROR
            }
        }
    }

    fun dismissErrorBanner() {
        _errorBanner.value = null
    }

    private fun mutateCurrentPage(transform: (StoryboardPage) -> StoryboardPage) {
        val pageNum = _currentPageNumber.value
        val current = _pagesMap.value[pageNum] ?: StoryboardPage.createBlank(pageNum, currentUserId)
        val updated = transform(current)
        val mapCopy = _pagesMap.value.toMutableMap()
        mapCopy[pageNum] = updated
        _pagesMap.value = mapCopy
        scheduleSavePage(updated)
    }

    private fun scheduleSavePage(page: StoryboardPage) {
        val pageNum = page.pageNumber
        saveJobs[pageNum]?.cancel()
        saveJobs[pageNum] = viewModelScope.launch {
            _syncStatus.value = SyncStatus.SAVING
            delay(350) // Debounce rapid keystrokes or strokes
            val latestPage = _pagesMap.value[pageNum] ?: page
            val result = repository.savePage(latestPage)
            if (result.isSuccess) {
                _syncStatus.value = SyncStatus.SAVED
                delay(700)
                _syncStatus.value = SyncStatus.AUTO_SAVED
            } else {
                _syncStatus.value = SyncStatus.ERROR
                _errorBanner.value = result.exceptionOrNull()?.localizedMessage ?: "Failed to save Page $pageNum"
            }
        }
    }

    // --- JSON EXPORT & IMPORT (Compatible with HTML Template Structure + Multi-Layer Sketch) ---

    fun exportProjectJson(): String {
        val root = JSONObject()
        val map = _pagesMap.value
        for (p in 1..25) {
            val d = map[p] ?: StoryboardPage.createBlank(p, currentUserId)
            val pageObj = JSONObject()
            pageObj.put("page", p)
            pageObj.put("completed", d.completed)
            val rulesObj = JSONObject().apply {
                put("dorsal", d.ruleDorsal)
                put("hood", d.ruleHood)
                put("tree", d.ruleTree)
                put("object", d.ruleObject)
            }
            pageObj.put("rules", rulesObj)
            pageObj.put("mandate", d.mandate)
            pageObj.put("cameraDistance", d.cameraDistance)
            pageObj.put("separationForce", d.separationForce)
            pageObj.put("storyline", d.storyline)
            pageObj.put("illustration", d.illustration)
            pageObj.put("kidPosture", d.kidPosture)
            pageObj.put("dogAction", d.dogAction)
            pageObj.put("distanceApart", d.distanceApart)
            pageObj.put("trigger", d.trigger)
            pageObj.put("response", d.response)
            pageObj.put("turningPoint", d.turningPoint)
            pageObj.put("terrain", d.terrain)
            pageObj.put("obstacle", d.obstacle)
            pageObj.put("sketchStrokes", d.sketchStrokes)
            root.put(p.toString(), pageObj)
        }
        return root.toString(2)
    }

    fun importProjectJson(rawJson: String, onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        try {
            val parsed = JSONObject(rawJson)
            val newMap = _pagesMap.value.toMutableMap()
            var importedCount = 0

            for (p in 1..25) {
                val pObj = parsed.optJSONObject(p.toString()) ?: continue
                val existing = newMap[p] ?: StoryboardPage.createBlank(p, currentUserId)
                val rulesObj = pObj.optJSONObject("rules")
                val updated = existing.copy(
                    pageNumber = p,
                    userId = currentUserId,
                    completed = pObj.optBoolean("completed", existing.completed),
                    ruleDorsal = rulesObj?.optBoolean("dorsal", existing.ruleDorsal)
                        ?: pObj.optBoolean("ruleDorsal", existing.ruleDorsal),
                    ruleHood = rulesObj?.optBoolean("hood", existing.ruleHood)
                        ?: pObj.optBoolean("ruleHood", existing.ruleHood),
                    ruleTree = rulesObj?.optBoolean("tree", existing.ruleTree)
                        ?: pObj.optBoolean("ruleTree", existing.ruleTree),
                    ruleObject = rulesObj?.optBoolean("object", existing.ruleObject)
                        ?: pObj.optBoolean("ruleObject", existing.ruleObject),
                    mandate = pObj.optString("mandate", existing.mandate),
                    cameraDistance = pObj.optString("cameraDistance", existing.cameraDistance),
                    separationForce = pObj.optString("separationForce", existing.separationForce),
                    storyline = pObj.optString("storyline", existing.storyline),
                    illustration = pObj.optString("illustration", existing.illustration),
                    kidPosture = pObj.optString("kidPosture", existing.kidPosture),
                    dogAction = pObj.optString("dogAction", existing.dogAction),
                    distanceApart = pObj.optString("distanceApart", existing.distanceApart),
                    trigger = pObj.optString("trigger", existing.trigger),
                    response = pObj.optString("response", existing.response),
                    turningPoint = pObj.optString("turningPoint", existing.turningPoint),
                    terrain = pObj.optString("terrain", existing.terrain),
                    obstacle = pObj.optString("obstacle", existing.obstacle),
                    sketchStrokes = pObj.optString("sketchStrokes", existing.sketchStrokes)
                )
                newMap[p] = updated
                importedCount++
            }

            _pagesMap.value = newMap
            val curPage = newMap[_currentPageNumber.value]
            if (curPage != null) {
                _sketchState.value = SketchCanvasState.deserialize(curPage.sketchStrokes)
            }

            viewModelScope.launch {
                _syncStatus.value = SyncStatus.SAVING
                for (p in 1..25) {
                    val page = newMap[p] ?: continue
                    repository.savePage(page)
                }
                _syncStatus.value = SyncStatus.SAVED
                delay(700)
                _syncStatus.value = SyncStatus.AUTO_SAVED
            }
            onComplete(true, "Imported $importedCount storyboard pages into Ship Telemetry.")
        } catch (e: Exception) {
            Log.e(TAG, "Invalid JSON import", e)
            onComplete(false, "Invalid JSON format: ${e.localizedMessage ?: "Parse error"}")
        }
    }

    private companion object {
        const val TAG = "StoryboardVM"
    }
}
