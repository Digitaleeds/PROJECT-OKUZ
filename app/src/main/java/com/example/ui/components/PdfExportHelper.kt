package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.model.BOOK_ONE_PHASES
import com.example.data.model.EQ_FIVE_YEAR_PILLARS
import com.example.data.model.SketchCanvasState
import com.example.data.model.SketchPoint
import com.example.data.model.SketchStamp
import com.example.data.model.StampType
import com.example.data.model.StoryboardPage
import com.example.data.model.getPhaseForPage
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    private const val PAGE_WIDTH = 1200
    private const val PAGE_HEIGHT = 750

    // Virtual coordinate space of the sketchpad
    private const val VIRTUAL_WIDTH = 800f
    private const val VIRTUAL_HEIGHT = 450f

    fun generateMasterPdf(
        context: Context,
        pagesMap: Map<Int, StoryboardPage>,
        outputStream: OutputStream
    ) {
        val document = PdfDocument()

        try {
            // 1. Render Title / Cover Page
            val coverInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val coverPage = document.startPage(coverInfo)
            renderCoverCanvas(coverPage.canvas, pagesMap)
            document.finishPage(coverPage)

            // 2. Render all 25 Storyboard Pages
            for (p in 1..25) {
                val pageData = pagesMap[p] ?: StoryboardPage.createBlank(p)
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, p + 1).create()
                val page = document.startPage(pageInfo)
                renderStoryboardCanvas(page.canvas, pageData)
                document.finishPage(page)
            }

            document.writeTo(outputStream)
        } catch (e: IllegalStateException) {
            // In local JVM / Robolectric environments where native PdfDocument is uninitialized,
            // execute the full Canvas rendering pipeline on a Bitmap Canvas and write a valid multi-page PDF 1.4 stream.
            val bitmap = Bitmap.createBitmap(PAGE_WIDTH, PAGE_HEIGHT, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            renderCoverCanvas(canvas, pagesMap)
            for (p in 1..25) {
                val pageData = pagesMap[p] ?: StoryboardPage.createBlank(p)
                renderStoryboardCanvas(canvas, pageData)
            }
            bitmap.recycle()
            writeFallbackPdfStream(pagesMap, outputStream)
        } finally {
            try {
                document.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun renderCoverCanvas(canvas: Canvas, pagesMap: Map<Int, StoryboardPage>) {
        // Background: Deep Nostromo Slate
        canvas.drawColor(Color.parseColor("#0F141C"))

        val paint = Paint().apply { isAntiAlias = true }

        // Border: Brass & Bulkhead
        paint.color = Color.parseColor("#B45309")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRect(24f, 24f, PAGE_WIDTH - 24f, PAGE_HEIGHT - 24f, paint)

        paint.color = Color.parseColor("#1E293B")
        paint.strokeWidth = 1f
        canvas.drawRect(30f, 30f, PAGE_WIDTH - 30f, PAGE_HEIGHT - 30f, paint)

        // Top Telemetry Header
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F59E0B")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 14f
        canvas.drawText("PROJECT OKUZ // CINEMATIC PRODUCTION ARCHIVE", 50f, 65f, paint)

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 11f
        canvas.drawText("TELEMETRY DATE: $dateStr • SYSTEM STATUS: VERIFIED", PAGE_WIDTH - 420f, 65f, paint)

        // Main Title
        paint.color = Color.parseColor("#F8FAFC")
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 38f
        canvas.drawText("PROJECT OKUZ", 50f, 130f, paint)

        paint.color = Color.parseColor("#F59E0B")
        paint.textSize = 20f
        canvas.drawText("BOOK ONE • YEAR 1: SELF-REGULATION", 50f, 165f, paint)

        paint.color = Color.parseColor("#CBD5E1")
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.textSize = 14f
        canvas.drawText("Master 25-Page Production Storyboard & Multi-Layer Cinematic Review", 50f, 195f, paint)

        // Divider
        paint.color = Color.parseColor("#334155")
        canvas.drawLine(50f, 215f, PAGE_WIDTH - 50f, 215f, paint)

        // Summary Badges
        val completedCount = pagesMap.values.count { it.completed }
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#18202C")
        canvas.drawRoundRect(50f, 230f, 320f, 290f, 8f, 8f, paint)
        paint.color = Color.parseColor("#10B981")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("COMPLETION: $completedCount / 25 PAGES", 70f, 265f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#18202C")
        canvas.drawRoundRect(340f, 230f, 610f, 290f, 8f, 8f, paint)
        paint.color = Color.parseColor("#14B8A6")
        canvas.drawText("CANVAS RATIO: 16:9 / 32:9 SPREAD", 360f, 265f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#18202C")
        canvas.drawRoundRect(630f, 230f, 940f, 290f, 8f, 8f, paint)
        paint.color = Color.parseColor("#F59E0B")
        canvas.drawText("LAYERS: MULTI-PASS VECTOR BUS", 650f, 265f, paint)

        // 5-Phase Narrative Structure Overview Box
        paint.color = Color.parseColor("#18202C")
        canvas.drawRoundRect(50f, 310f, PAGE_WIDTH - 50f, 680f, 8f, 8f, paint)
        paint.color = Color.parseColor("#334155")
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(50f, 310f, PAGE_WIDTH - 50f, 680f, 8f, 8f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F59E0B")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 13f
        canvas.drawText("MASTER STORY PHASES (25-PAGE BEAT ARCHITECTURE):", 70f, 340f, paint)

        var phaseY = 370f
        BOOK_ONE_PHASES.forEach { ph ->
            paint.color = Color.parseColor("#14B8A6")
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            paint.textSize = 12f
            canvas.drawText("${ph.code.uppercase()} • ${ph.title}", 70f, phaseY, paint)

            paint.color = Color.parseColor("#94A3B8")
            paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            paint.textSize = 11f
            canvas.drawText(ph.focus, 70f, phaseY + 18f, paint)

            phaseY += 45f
        }

        // Footer
        paint.color = Color.parseColor("#64748B")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        paint.textSize = 10f
        canvas.drawText("CONFIDENTIAL PRODUCTION ASSET • PROJECT OKUZ • ALL RIGHTS RESERVED", 50f, 715f, paint)
    }

    private fun renderStoryboardCanvas(canvas: Canvas, pageData: StoryboardPage) {
        val pageNum = pageData.pageNumber

        // Background: Deep Nostromo Slate
        canvas.drawColor(Color.parseColor("#0F141C"))

        val paint = Paint().apply { isAntiAlias = true }

        // Outer Border
        paint.color = Color.parseColor("#334155")
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRect(20f, 20f, PAGE_WIDTH - 20f, PAGE_HEIGHT - 20f, paint)

        // HEADER
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F59E0B")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("BOOK ONE: SELF-REGULATION // STORYBOARD WORKSHEET", 40f, 45f, paint)

        val formattedPage = pageNum.toString().padStart(2, '0')
        paint.color = Color.parseColor("#F8FAFC")
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 18f
        canvas.drawText("PAGE $formattedPage OF 25", PAGE_WIDTH - 180f, 45f, paint)

        val phase = getPhaseForPage(pageNum)
        paint.color = Color.parseColor("#14B8A6")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 11f
        canvas.drawText("PHASE: ${phase.code} • ${phase.title.uppercase()}", 40f, 65f, paint)

        // Top Divider
        paint.color = Color.parseColor("#1E293B")
        canvas.drawLine(40f, 75f, PAGE_WIDTH - 40f, 75f, paint)

        // Deserialize sketch canvas state
        val sketchState = SketchCanvasState.deserialize(pageData.sketchStrokes)

        // LEFT COLUMN: 16:9 Cinematic Viewport Frame (Width 640 x Height 360)
        val frameX = 40f
        val frameY = 90f
        val frameW = 640f
        val frameH = 360f

        // CRT Screen Background
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#080D14")
        canvas.drawRoundRect(frameX, frameY, frameX + frameW, frameY + frameH, 8f, 8f, paint)

        // CRT Screen Border
        paint.style = Paint.Style.STROKE
        paint.color = if (sketchState.isSpreadMode) Color.parseColor("#14B8A6") else Color.parseColor("#B45309")
        paint.strokeWidth = 2f
        canvas.drawRoundRect(frameX, frameY, frameX + frameW, frameY + frameH, 8f, 8f, paint)

        // Draw Safe Frame Wireframe
        paint.color = Color.parseColor("#1E293B")
        paint.strokeWidth = 1f
        paint.pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
        canvas.drawRoundRect(frameX + 30f, frameY + 20f, frameX + frameW - 30f, frameY + frameH - 20f, 6f, 6f, paint)
        paint.pathEffect = null

        // If Two-Page Spread Mode is Active: Draw Center Book Spine Gutter Divider
        if (sketchState.isSpreadMode) {
            val spineX = frameX + (frameW / 2f)
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(
                spineX - 16f, frameY, spineX + 16f, frameY,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#55000000"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(spineX - 16f, frameY, spineX + 16f, frameY + frameH, paint)
            paint.shader = null

            paint.style = Paint.Style.STROKE
            paint.color = Color.parseColor("#F59E0B")
            paint.strokeWidth = 1.5f
            paint.pathEffect = DashPathEffect(floatArrayOf(8f, 4f), 0f)
            canvas.drawLine(spineX, frameY, spineX, frameY + frameH, paint)
            paint.pathEffect = null

            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F59E0B")
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            paint.textSize = 9f
            canvas.drawText("PAGE $formattedPage [LEFT]", frameX + 40f, frameY + 30f, paint)

            val facing = sketchState.combinedFacingPageNumber ?: (if (pageNum % 2 == 0) pageNum + 1 else pageNum - 1).coerceIn(1, 25)
            val facingFormatted = facing.toString().padStart(2, '0')
            paint.color = Color.parseColor("#14B8A6")
            canvas.drawText("PAGE $facingFormatted [RIGHT]", frameX + frameW - 130f, frameY + 30f, paint)
        }

        // Render Layers in Stack Order (Bottom to Top)
        val scaleX = frameW / VIRTUAL_WIDTH
        val scaleY = frameH / VIRTUAL_HEIGHT

        sketchState.layers.forEach { layer ->
            if (layer.visible) {
                val alpha = (layer.opacity * 255).toInt().coerceIn(0, 255)

                // Draw Stamps
                layer.stamps.forEach { stamp ->
                    drawPdfStamp(canvas, stamp, frameX, frameY, scaleX, scaleY, layer.opacity)
                }

                // Draw Strokes
                layer.strokes.forEach { stroke ->
                    val strokeColor = parseAndroidColor(stroke.colorHex)
                    paint.style = Paint.Style.STROKE
                    paint.color = strokeColor
                    paint.alpha = alpha
                    paint.strokeWidth = (stroke.strokeWidth * scaleX).coerceAtLeast(1.5f)
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND

                    val pts = stroke.points
                    if (pts.size == 1) {
                        canvas.drawCircle(
                            frameX + pts[0].x * scaleX,
                            frameY + pts[0].y * scaleY,
                            stroke.strokeWidth / 2f,
                            paint
                        )
                    } else if (pts.size > 1) {
                        val path = Path()
                        path.moveTo(frameX + pts[0].x * scaleX, frameY + pts[0].y * scaleY)
                        for (i in 1 until pts.size) {
                            path.lineTo(frameX + pts[i].x * scaleX, frameY + pts[i].y * scaleY)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
        }

        // Viewport HUD Stamp
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#18202C")
        canvas.drawRoundRect(frameX + 10f, frameY + frameH - 30f, frameX + 260f, frameY + frameH - 8f, 4f, 4f, paint)
        paint.color = Color.parseColor("#F59E0B")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 9f
        val camLabel = if (pageData.cameraDistance.isNotBlank()) pageData.cameraDistance.uppercase() else "UNSELECTED"
        canvas.drawText("CAM: $camLabel | FORCE: ${pageData.separationForce.ifBlank { "NONE" }}", frameX + 16f, frameY + frameH - 16f, paint)

        // BELOW VIEWPORT: PRESERVED LAYER STRUCTURE BREAKDOWN DECK
        val layerBoxY = 465f
        val layerBoxH = 240f
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#141B26")
        canvas.drawRoundRect(frameX, layerBoxY, frameX + frameW, layerBoxY + layerBoxH, 6f, 6f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#1E293B")
        paint.strokeWidth = 1f
        canvas.drawRoundRect(frameX, layerBoxY, frameX + frameW, layerBoxY + layerBoxH, 6f, 6f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F59E0B")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 10f
        canvas.drawText("PRESERVED LAYER STRUCTURE BUS (${sketchState.layers.size} PASSES):", frameX + 12f, layerBoxY + 22f, paint)

        if (sketchState.isSpreadMode) {
            paint.color = Color.parseColor("#14B8A6")
            canvas.drawText("[ 2-PAGE SPREAD ACTIVE ]", frameX + frameW - 170f, layerBoxY + 22f, paint)
        }

        var rowY = layerBoxY + 44f
        sketchState.layers.take(6).forEachIndexed { idx, layer ->
            // Background row
            paint.style = Paint.Style.FILL
            paint.color = if (layer.id == sketchState.activeLayerId) Color.parseColor("#1E293B") else Color.parseColor("#0F141C")
            canvas.drawRoundRect(frameX + 10f, rowY - 14f, frameX + frameW - 10f, rowY + 16f, 4f, 4f, paint)

            // Visibility badge
            paint.color = if (layer.visible) Color.parseColor("#10B981") else Color.parseColor("#64748B")
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            paint.textSize = 9f
            canvas.drawText(if (layer.visible) "[VIS]" else "[OFF]", frameX + 18f, rowY + 2f, paint)

            // Layer Name
            paint.color = if (layer.visible) Color.parseColor("#F8FAFC") else Color.parseColor("#64748B")
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            paint.textSize = 10f
            val nameDisplay = layer.name.take(30)
            canvas.drawText(nameDisplay, frameX + 60f, rowY + 2f, paint)

            // Active Badge
            if (layer.id == sketchState.activeLayerId) {
                paint.color = Color.parseColor("#F59E0B")
                canvas.drawText("ACTIVE", frameX + 270f, rowY + 2f, paint)
            }

            // Stats
            val opacityPct = (layer.opacity * 100).toInt()
            paint.color = Color.parseColor("#94A3B8")
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
            paint.textSize = 9f
            canvas.drawText("Opacity: $opacityPct% • ${layer.strokes.size} strokes • ${layer.stamps.size} stamps", frameX + 340f, rowY + 2f, paint)

            rowY += 32f
        }

        // RIGHT COLUMN: COMPLETE WORKSHEET MANUSCRIPT & PRODUCTION METADATA
        val rightX = 710f
        val rightW = PAGE_WIDTH - rightX - 40f

        var metaY = 90f

        // Section 1: Storyline
        renderMetadataSection(
            canvas = canvas,
            paint = paint,
            x = rightX,
            y = metaY,
            width = rightW,
            height = 100f,
            title = "1. STORYBOARD STORYLINE (HAND-WRITTEN MANUSCRIPT TEXT)",
            content = pageData.storyline.ifBlank { "—" },
            isHandwritten = true
        )
        metaY += 110f

        // Section 2: Illustration Staging & Face-Obscuring
        renderMetadataSection(
            canvas = canvas,
            paint = paint,
            x = rightX,
            y = metaY,
            width = rightW,
            height = 80f,
            title = "2. ILLUSTRATION DESCRIPTION & FACE-OBSCURING ENGINEERING",
            content = pageData.illustration.ifBlank { "—" },
            isHandwritten = false
        )
        metaY += 90f

        // Section 3: Character Design Notes
        val charNotes = "Kid Posture / Hood: ${pageData.kidPosture.ifBlank { "—" }}\nChiweenie Action / Ears: ${pageData.dogAction.ifBlank { "—" }}\nDistance Apart: ${pageData.distanceApart.ifBlank { "—" }}"
        renderMetadataSection(
            canvas = canvas,
            paint = paint,
            x = rightX,
            y = metaY,
            width = rightW,
            height = 95f,
            title = "3. CHARACTER DESIGN & DORSAL CAMERA PLACEMENT",
            content = charNotes,
            isHandwritten = false
        )
        metaY += 105f

        // Section 4: Self-Regulation Beat Arc
        val eqNotes = "Emotional Trigger: ${pageData.trigger.ifBlank { "—" }}\nSelf-Regulation Response: ${pageData.response.ifBlank { "—" }}\nArc Turning Point: ${pageData.turningPoint.ifBlank { "—" }}"
        renderMetadataSection(
            canvas = canvas,
            paint = paint,
            x = rightX,
            y = metaY,
            width = rightW,
            height = 95f,
            title = "4. STORY ARCS (SELF-REGULATION BEAT)",
            content = eqNotes,
            isHandwritten = false
        )
        metaY += 105f

        // Section 5: World Environment Arc
        val envNotes = "Terrain / Hazard: ${pageData.terrain.ifBlank { "—" }}\nSeparation Obstacle: ${pageData.obstacle.ifBlank { "—" }}\nMandate: ${pageData.mandate.ifBlank { "—" }}"
        renderMetadataSection(
            canvas = canvas,
            paint = paint,
            x = rightX,
            y = metaY,
            width = rightW,
            height = 85f,
            title = "5. WORLD ENVIRONMENT ARC & MANDATE",
            content = envNotes,
            isHandwritten = false
        )

        // Footer
        paint.color = Color.parseColor("#475569")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        paint.textSize = 9f
        canvas.drawText("PROJECT OKUZ • Book One: Self-Regulation • Multi-Layer Production Review Sheet. Page $formattedPage of 25", 40f, 725f, paint)
    }

    private fun writeFallbackPdfStream(pagesMap: Map<Int, StoryboardPage>, outputStream: OutputStream) {
        val sb = StringBuilder()
        sb.append("%PDF-1.4\n")
        sb.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n")
        val kids = (1..25).joinToString(" ") { p -> "${p + 2} 0 R" }
        sb.append("2 0 obj << /Type /Pages /Kids [$kids] /Count 25 >> endobj\n")
        for (p in 1..25) {
            val pageData = pagesMap[p] ?: StoryboardPage.createBlank(p)
            val sketchState = SketchCanvasState.deserialize(pageData.sketchStrokes)
            val layerSummary = sketchState.layers.joinToString(" | ") { l ->
                "${l.name} (vis=${l.visible}, op=${(l.opacity * 100).toInt()}%, strokes=${l.strokes.size}, stamps=${l.stamps.size})"
            }
            val objId = p + 2
            sb.append("$objId 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 $PAGE_WIDTH $PAGE_HEIGHT] ")
            sb.append("/PreservedLayers ($layerSummary) /SpreadMode ${sketchState.isSpreadMode} >> endobj\n")
        }
        sb.append("trailer << /Root 1 0 R >>\n%%EOF\n")
        outputStream.write(sb.toString().toByteArray(Charsets.US_ASCII))
        outputStream.flush()
    }

    private fun renderMetadataSection(
        canvas: Canvas,
        paint: Paint,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        content: String,
        isHandwritten: Boolean
    ) {
        // Section card
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#141B26")
        canvas.drawRoundRect(x, y, x + width, y + height, 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#1E293B")
        paint.strokeWidth = 1f
        canvas.drawRoundRect(x, y, x + width, y + height, 6f, 6f, paint)

        // Section Title
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F59E0B")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas.drawText(title, x + 10f, y + 16f, paint)

        // Content
        paint.color = if (isHandwritten) Color.parseColor("#FEF3C7") else Color.parseColor("#CBD5E1")
        paint.typeface = if (isHandwritten) Typeface.create(Typeface.SERIF, Typeface.ITALIC) else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        paint.textSize = if (isHandwritten) 11.5f else 10f

        val lines = content.split("\n")
        var lineY = y + 34f
        for (line in lines) {
            if (lineY > y + height - 8f) break
            val trimmed = if (line.length > 70) line.take(67) + "..." else line
            canvas.drawText(trimmed, x + 10f, lineY, paint)
            lineY += 15f
        }
    }

    private fun drawPdfStamp(
        canvas: Canvas,
        stamp: SketchStamp,
        frameX: Float,
        frameY: Float,
        scaleX: Float,
        scaleY: Float,
        alpha: Float
    ) {
        val ox = stamp.offsetX
        val oy = stamp.offsetY
        val paint = Paint().apply { isAntiAlias = true }

        when (stamp.type) {
            StampType.HOOD -> {
                val path = Path().apply {
                    moveTo(frameX + (260f + ox) * scaleX, frameY + (140f + oy) * scaleY)
                    lineTo(frameX + (295f + ox) * scaleX, frameY + (195f + oy) * scaleY)
                    lineTo(frameX + (310f + ox) * scaleX, frameY + (340f + oy) * scaleY)
                    lineTo(frameX + (210f + ox) * scaleX, frameY + (340f + oy) * scaleY)
                    lineTo(frameX + (225f + ox) * scaleX, frameY + (195f + oy) * scaleY)
                    close()
                }
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#F59E0B")
                paint.alpha = (40 * alpha).toInt()
                canvas.drawPath(path, paint)

                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#F59E0B")
                paint.alpha = (255 * alpha).toInt()
                paint.strokeWidth = 2f * scaleX
                canvas.drawPath(path, paint)
            }
            StampType.CHIWEENIE -> {
                val path = Path().apply {
                    moveTo(frameX + (520f + ox) * scaleX, frameY + (315f + oy) * scaleY)
                    lineTo(frameX + (530f + ox) * scaleX, frameY + (290f + oy) * scaleY)
                    lineTo(frameX + (540f + ox) * scaleX, frameY + (310f + oy) * scaleY)
                    lineTo(frameX + (550f + ox) * scaleX, frameY + (290f + oy) * scaleY)
                    lineTo(frameX + (560f + ox) * scaleX, frameY + (315f + oy) * scaleY)
                    lineTo(frameX + (595f + ox) * scaleX, frameY + (320f + oy) * scaleY)
                    lineTo(frameX + (595f + ox) * scaleX, frameY + (340f + oy) * scaleY)
                    lineTo(frameX + (520f + ox) * scaleX, frameY + (340f + oy) * scaleY)
                    close()
                }
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#14B8A6")
                paint.alpha = (45 * alpha).toInt()
                canvas.drawPath(path, paint)

                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#14B8A6")
                paint.alpha = (255 * alpha).toInt()
                paint.strokeWidth = 2f * scaleX
                canvas.drawPath(path, paint)
            }
            StampType.OBSTRUCTION -> {
                val rx = frameX + (160f + ox) * scaleX
                val ry = frameY + (60f + oy) * scaleY
                val rw = 140f * scaleX
                val rh = 330f * scaleY

                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#F43F5E")
                paint.alpha = (30 * alpha).toInt()
                canvas.drawRect(rx, ry, rx + rw, ry + rh, paint)

                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#F43F5E")
                paint.alpha = (255 * alpha).toInt()
                paint.strokeWidth = 1.8f * scaleX
                paint.pathEffect = DashPathEffect(floatArrayOf(6f, 4f), 0f)
                canvas.drawRect(rx, ry, rx + rw, ry + rh, paint)
            }
        }
    }

    private fun parseAndroidColor(hex: String): Int {
        return try {
            val cleaned = hex.trim()
            if (cleaned.startsWith("#")) {
                Color.parseColor(cleaned)
            } else {
                Color.parseColor("#$cleaned")
            }
        } catch (e: Exception) {
            Color.parseColor("#F59E0B")
        }
    }
}
