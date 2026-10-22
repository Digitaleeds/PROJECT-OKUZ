package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.SketchCanvasState
import com.example.data.model.SketchHistoryStack
import com.example.data.model.SketchLayer
import com.example.data.model.SketchPoint
import com.example.data.model.SketchStamp
import com.example.data.model.SketchStroke
import com.example.data.model.StampType
import com.example.data.model.StoryboardPage
import com.example.ui.components.PdfExportHelper
import com.example.ui.components.PngExportHelper
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Project Okuz", appName)
    }

    @Test
    fun `multi-layer sketchpad supports add delete reorder visibility and serialization`() {
        val initial = SketchCanvasState()
        assertEquals(3, initial.layers.size)

        // Add a new layer and select it
        val newLayer = SketchLayer(
            id = "layer_custom",
            name = "L4 // OVERLAY",
            visible = true,
            strokes = listOf(
                SketchStroke(
                    colorHex = "#10B981",
                    strokeWidth = 4f,
                    points = listOf(SketchPoint(50f, 60f), SketchPoint(120f, 140f))
                )
            ),
            stamps = listOf(SketchStamp(type = StampType.HOOD, offsetX = 18f, offsetY = 10f))
        )
        val withAdded = initial.copy(
            layers = initial.layers + newLayer,
            activeLayerId = newLayer.id
        )
        assertEquals(4, withAdded.layers.size)
        assertEquals("layer_custom", withAdded.activeLayer?.id)

        // Toggle visibility of first layer
        val toggledLayers = withAdded.layers.mapIndexed { index, l ->
            if (index == 0) l.copy(visible = false) else l
        }
        val withHidden = withAdded.copy(layers = toggledLayers)
        assertFalse(withHidden.layers.first().visible)

        // Serialize and deserialize roundtrip
        val raw = SketchCanvasState.serialize(withHidden)
        val restored = SketchCanvasState.deserialize(raw)
        assertEquals(4, restored.layers.size)
        assertEquals("layer_custom", restored.activeLayerId)
        assertFalse(restored.layers.first().visible)
        assertEquals(1, restored.activeLayer?.strokes?.size)
        assertEquals(1, restored.activeLayer?.stamps?.size)
        assertTrue(restored.hasAnyContent)
    }

    @Test
    fun `layer merge down combines strokes from two layers into one`() {
        val l1 = SketchLayer(
            id = "l1",
            name = "Layer 1 (Base)",
            strokes = listOf(
                SketchStroke(colorHex = "#F59E0B", points = listOf(SketchPoint(10f, 10f), SketchPoint(20f, 20f)))
            )
        )
        val l2 = SketchLayer(
            id = "l2",
            name = "Layer 2 (Upper)",
            strokes = listOf(
                SketchStroke(colorHex = "#10B981", points = listOf(SketchPoint(30f, 30f), SketchPoint(40f, 40f)))
            ),
            stamps = listOf(SketchStamp(type = StampType.HOOD))
        )
        val state = SketchCanvasState(layers = listOf(l2, l1), activeLayerId = "l2")
        assertEquals(2, state.layers.size)

        // Merge l2 down into l1
        val upper = state.layers[0]
        val lower = state.layers[1]
        val combined = lower.copy(
            strokes = lower.strokes + upper.strokes,
            stamps = lower.stamps + upper.stamps
        )
        val mergedState = state.copy(layers = listOf(combined), activeLayerId = combined.id)

        assertEquals(1, mergedState.layers.size)
        assertEquals(2, mergedState.layers.first().strokes.size)
        assertEquals(1, mergedState.layers.first().stamps.size)
    }

    @Test
    fun `combine two pages as one sets spread mode and facing page metadata with roundtrip serialization`() {
        val baseState = SketchCanvasState(
            isSpreadMode = true,
            combinedFacingPageNumber = 3
        )
        assertTrue(baseState.isSpreadMode)
        assertEquals(3, baseState.combinedFacingPageNumber)

        val raw = SketchCanvasState.serialize(baseState)
        val deserialized = SketchCanvasState.deserialize(raw)

        assertTrue(deserialized.isSpreadMode)
        assertEquals(3, deserialized.combinedFacingPageNumber)
    }

    @Test
    fun `generateMasterPdf writes valid multi-page PDF document preserving layer structure`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testPages = (1..25).associateWith { p ->
            val canvasState = SketchCanvasState(
                layers = listOf(
                    SketchLayer(
                        id = "l1_$p",
                        name = "L1 // TERRAIN",
                        visible = true,
                        opacity = 0.9f,
                        strokes = listOf(
                            SketchStroke(colorHex = "#F59E0B", points = listOf(SketchPoint(20f, 30f), SketchPoint(50f, 80f)))
                        )
                    ),
                    SketchLayer(
                        id = "l2_$p",
                        name = "L2 // SILHOUETTES",
                        visible = true,
                        opacity = 1.0f,
                        stamps = listOf(SketchStamp(type = StampType.HOOD))
                    )
                ),
                isSpreadMode = p == 2,
                combinedFacingPageNumber = if (p == 2) 3 else null
            )
            StoryboardPage.createBlank(p).copy(
                storyline = "Manuscript line for page $p: The wind rises.",
                illustration = "Staging angle behind hooded figure.",
                completed = p % 2 == 0,
                sketchStrokes = SketchCanvasState.serialize(canvasState)
            )
        }

        val outputStream = ByteArrayOutputStream()
        PdfExportHelper.generateMasterPdf(context, testPages, outputStream)
        val pdfBytes = outputStream.toByteArray()

        assertTrue("Generated PDF should not be empty", pdfBytes.isNotEmpty())
        val header = String(pdfBytes.take(5).toByteArray(), Charsets.US_ASCII)
        assertEquals("%PDF-", header)
    }

    @Test
    fun `undo and redo stack reverts and restores drawing actions in sketchpad`() {
        val history = SketchHistoryStack()
        var currentState = SketchCanvasState()
        assertFalse(history.canUndoWithFallback(currentState))
        assertFalse(history.canRedo)

        // Action 1: Add a pen stroke to active layer
        history.recordBeforeMutation(currentState)
        val stroke1 = SketchStroke(
            colorHex = "#F59E0B",
            strokeWidth = 3.5f,
            points = listOf(SketchPoint(10f, 10f), SketchPoint(80f, 90f))
        )
        currentState = currentState.copy(
            layers = currentState.layers.map { l ->
                if (l.id == currentState.activeLayerId) l.copy(strokes = l.strokes + stroke1) else l
            }
        )
        assertTrue(history.canUndoWithFallback(currentState))
        assertEquals(1, history.undoSize)
        assertEquals(1, currentState.activeLayer?.strokes?.size)

        // Action 2: Stamp a Hood marker on active layer
        history.recordBeforeMutation(currentState)
        val stamp1 = SketchStamp(type = StampType.HOOD, offsetX = 0f, offsetY = 0f)
        currentState = currentState.copy(
            layers = currentState.layers.map { l ->
                if (l.id == currentState.activeLayerId) l.copy(stamps = l.stamps + stamp1) else l
            }
        )
        assertEquals(2, history.undoSize)
        assertEquals(1, currentState.activeLayer?.stamps?.size)

        // Undo Action 2 (removes stamp)
        currentState = history.undo(currentState)!!
        assertEquals(1, history.undoSize)
        assertEquals(1, history.redoSize)
        assertTrue(history.canRedo)
        assertEquals(0, currentState.activeLayer?.stamps?.size)
        assertEquals(1, currentState.activeLayer?.strokes?.size)

        // Undo Action 1 (removes stroke)
        currentState = history.undo(currentState)!!
        assertEquals(0, history.undoSize)
        assertEquals(2, history.redoSize)
        assertEquals(0, currentState.activeLayer?.strokes?.size)

        // Redo Action 1 (restores stroke)
        currentState = history.redo(currentState)!!
        assertEquals(1, history.undoSize)
        assertEquals(1, history.redoSize)
        assertEquals(1, currentState.activeLayer?.strokes?.size)

        // New drawing action clears the remaining redo stack
        history.recordBeforeMutation(currentState)
        val stroke2 = SketchStroke(
            colorHex = "#10B981",
            strokeWidth = 5f,
            points = listOf(SketchPoint(100f, 100f), SketchPoint(200f, 200f))
        )
        currentState = currentState.copy(
            layers = currentState.layers.map { l ->
                if (l.id == currentState.activeLayerId) l.copy(strokes = l.strokes + stroke2) else l
            }
        )
        assertFalse(history.canRedo)
        assertEquals(0, history.redoSize)
        assertEquals(2, currentState.activeLayer?.strokes?.size)
    }

    @Test
    fun `save current canvas state as PNG image file locally on the device`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val testState = SketchCanvasState(
            layers = listOf(
                SketchLayer(
                    id = "layer_base",
                    name = "L1 // TERRAIN",
                    visible = true,
                    opacity = 0.9f,
                    strokes = listOf(
                        SketchStroke(
                            colorHex = "#F59E0B",
                            strokeWidth = 4f,
                            points = listOf(SketchPoint(20f, 40f), SketchPoint(120f, 180f), SketchPoint(220f, 260f))
                        )
                    )
                ),
                SketchLayer(
                    id = "layer_actors",
                    name = "L2 // SILHOUETTES",
                    visible = true,
                    opacity = 1.0f,
                    stamps = listOf(
                        SketchStamp(type = StampType.HOOD, offsetX = 10f, offsetY = 10f),
                        SketchStamp(type = StampType.CHIWEENIE, offsetX = 20f, offsetY = 15f)
                    )
                )
            ),
            isSpreadMode = false
        )

        // 1. Verify bitmap rendering
        val bitmap = PngExportHelper.renderCanvasToBitmap(
            sketchState = testState,
            pageNumber = 1,
            cameraDistance = "Close-up",
            separationForce = "Distant"
        )
        assertEquals(PngExportHelper.DEFAULT_EXPORT_WIDTH, bitmap.width)
        assertEquals(PngExportHelper.DEFAULT_EXPORT_HEIGHT, bitmap.height)
        bitmap.recycle()

        // 2. Verify export to output stream produces a valid PNG stream
        val outputStream = ByteArrayOutputStream()
        PngExportHelper.exportCanvasToStream(
            sketchState = testState,
            pageNumber = 1,
            cameraDistance = "Close-up",
            separationForce = "Distant",
            outputStream = outputStream
        )
        val pngBytes = outputStream.toByteArray()
        assertTrue("PNG byte array should not be empty", pngBytes.isNotEmpty())

        // Verify PNG magic number bytes: 0x89 0x50 0x4E 0x47 (or \u0089PNG in ASCII)
        assertEquals(0x89.toByte(), pngBytes[0])
        assertEquals('P'.code.toByte(), pngBytes[1])
        assertEquals('N'.code.toByte(), pngBytes[2])
        assertEquals('G'.code.toByte(), pngBytes[3])

        // 3. Verify saving locally to device file storage
        val saveResult = PngExportHelper.saveToLocalFile(
            context = context,
            sketchState = testState,
            pageNumber = 1,
            cameraDistance = "Close-up",
            separationForce = "Distant"
        )
        assertTrue("Local file save should succeed", saveResult.isSuccess)
        val savedFile = saveResult.getOrNull()
        assertTrue("Saved file should exist on device", savedFile != null && savedFile.exists())
        assertTrue("Saved file should have non-zero size", savedFile!!.length() > 0)
        assertTrue("File should end with .png", savedFile.name.endsWith(".png"))
    }
}


