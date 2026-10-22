package com.example.ui.components

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.SketchCanvasState
import com.example.data.model.SketchStamp
import com.example.data.model.StampType
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PngExportHelper {

    private const val VIRTUAL_WIDTH = 800f
    private const val VIRTUAL_HEIGHT = 450f

    // Standard 16:9 output resolution
    const val DEFAULT_EXPORT_WIDTH = 1600
    const val DEFAULT_EXPORT_HEIGHT = 900

    /**
     * Renders the current SketchCanvasState to an Android Bitmap.
     * Preserves all layer visibility, layer opacities, strokes, template stamps,
     * and two-page spread formatting.
     */
    fun renderCanvasToBitmap(
        sketchState: SketchCanvasState,
        pageNumber: Int,
        cameraDistance: String = "",
        separationForce: String = "",
        exportWidth: Int = DEFAULT_EXPORT_WIDTH,
        exportHeight: Int = DEFAULT_EXPORT_HEIGHT
    ): Bitmap {
        val width = if (sketchState.isSpreadMode) exportWidth * 2 else exportWidth
        val height = exportHeight

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Background: Deep CRT Nostromo Dark
        canvas.drawColor(Color.parseColor("#080D14"))

        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
        }

        // 2. Viewport Border
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = if (sketchState.isSpreadMode) Color.parseColor("#14B8A6") else Color.parseColor("#B45309")
        canvas.drawRect(8f, 8f, width - 8f, height - 8f, paint)

        // 3. Safe Framing Guideline (Dashed Grid)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1.5f
        paint.color = Color.parseColor("#1E293B")
        paint.pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        canvas.drawRect(40f, 30f, width - 40f, height - 30f, paint)
        paint.pathEffect = null

        // 4. Two-Page Spread Center Book Spine Gutter Divider
        val scaleX = width.toFloat() / VIRTUAL_WIDTH
        val scaleY = height.toFloat() / VIRTUAL_HEIGHT

        if (sketchState.isSpreadMode) {
            val spineX = width / 2f

            // Shaded spine shadow
            paint.style = Paint.Style.FILL
            paint.shader = LinearGradient(
                spineX - 32f, 0f, spineX + 32f, 0f,
                intArrayOf(Color.TRANSPARENT, Color.parseColor("#77000000"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.drawRect(spineX - 32f, 0f, spineX + 32f, height.toFloat(), paint)
            paint.shader = null

            // Spine dashed divider line
            paint.style = Paint.Style.STROKE
            paint.color = Color.parseColor("#F59E0B")
            paint.strokeWidth = 2.5f
            paint.pathEffect = DashPathEffect(floatArrayOf(14f, 8f), 0f)
            canvas.drawLine(spineX, 0f, spineX, height.toFloat(), paint)
            paint.pathEffect = null

            // Left / Right Page Spread Indicators
            paint.style = Paint.Style.FILL
            paint.color = Color.parseColor("#F59E0B")
            paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            paint.textSize = 18f
            val formattedCur = pageNumber.toString().padStart(2, '0')
            canvas.drawText("PAGE $formattedCur [LEFT PAGE]", 60f, 60f, paint)

            val facing = sketchState.combinedFacingPageNumber
                ?: (if (pageNumber % 2 == 0) pageNumber + 1 else pageNumber - 1).coerceIn(1, 25)
            val formattedFacing = facing.toString().padStart(2, '0')
            paint.color = Color.parseColor("#14B8A6")
            canvas.drawText("PAGE $formattedFacing [RIGHT PAGE]", width - 360f, 60f, paint)
        }

        // 5. Draw Layers from Bottom to Top in Stack Order
        sketchState.layers.forEach { layer ->
            if (layer.visible) {
                val layerAlpha = (layer.opacity * 255).toInt().coerceIn(0, 255)

                // A. Draw Stamps on this layer
                layer.stamps.forEach { stamp ->
                    drawStamp(canvas, stamp, scaleX, scaleY, layer.opacity)
                }

                // B. Draw Strokes on this layer
                layer.strokes.forEach { stroke ->
                    val strokeColor = parseAndroidColor(stroke.colorHex)
                    paint.style = Paint.Style.STROKE
                    paint.color = strokeColor
                    paint.alpha = layerAlpha
                    paint.strokeWidth = (stroke.strokeWidth * (scaleX + scaleY) / 2f).coerceAtLeast(2f)
                    paint.strokeCap = Paint.Cap.ROUND
                    paint.strokeJoin = Paint.Join.ROUND

                    val pts = stroke.points
                    if (pts.size == 1) {
                        canvas.drawCircle(
                            pts[0].x * scaleX,
                            pts[0].y * scaleY,
                            stroke.strokeWidth * scaleX / 2f,
                            paint
                        )
                    } else if (pts.size > 1) {
                        val path = Path()
                        path.moveTo(pts[0].x * scaleX, pts[0].y * scaleY)
                        for (i in 1 until pts.size) {
                            path.lineTo(pts[i].x * scaleX, pts[i].y * scaleY)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
        }

        // 6. Viewport HUD Telemetry Banner
        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#CC0F172A")
        canvas.drawRoundRect(24f, height - 52f, 540f, height - 16f, 6f, 6f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.parseColor("#334155")
        paint.strokeWidth = 1f
        canvas.drawRoundRect(24f, height - 52f, 540f, height - 16f, 6f, 6f, paint)

        paint.style = Paint.Style.FILL
        paint.color = Color.parseColor("#F59E0B")
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = 14f

        val formatted = pageNumber.toString().padStart(2, '0')
        val cam = cameraDistance.ifBlank { "UNSELECTED" }.uppercase()
        val force = separationForce.ifBlank { "NONE" }.uppercase()
        val spreadNotice = if (sketchState.isSpreadMode) " | 2-PAGE SPREAD" else ""
        canvas.drawText("P$formatted // CAM: $cam // FORCE: $force$spreadNotice", 36f, height - 30f, paint)

        // Watermark on Top-Right
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)
        canvas.drawText("PROJECT OKUZ • NOSTROMO CRT 16:9", width - 300f, height - 30f, paint)

        return bitmap
    }

    /**
     * Compresses and writes the rendered canvas to an output stream as a PNG.
     */
    fun exportCanvasToStream(
        sketchState: SketchCanvasState,
        pageNumber: Int,
        cameraDistance: String = "",
        separationForce: String = "",
        outputStream: OutputStream
    ) {
        val bitmap = renderCanvasToBitmap(
            sketchState = sketchState,
            pageNumber = pageNumber,
            cameraDistance = cameraDistance,
            separationForce = separationForce
        )
        try {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Saves the current canvas state directly to the local device's Pictures gallery
     * using MediaStore (Android 10+ scoped storage compliant), making it immediately
     * viewable in Google Photos, Gallery, and local file managers.
     */
    fun saveToDevicePictures(
        context: Context,
        sketchState: SketchCanvasState,
        pageNumber: Int,
        cameraDistance: String = "",
        separationForce: String = ""
    ): Result<Uri> {
        return runCatching {
            val formatted = pageNumber.toString().padStart(2, '0')
            val timestamp = System.currentTimeMillis()
            val fileName = "project_okuz_p${formatted}_sketch_$timestamp.png"

            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/ProjectOkuz")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: throw IllegalStateException("Failed to create MediaStore entry in Pictures")

            resolver.openOutputStream(uri)?.use { out ->
                exportCanvasToStream(
                    sketchState = sketchState,
                    pageNumber = pageNumber,
                    cameraDistance = cameraDistance,
                    separationForce = separationForce,
                    outputStream = out
                )
            } ?: throw IllegalStateException("Failed to open output stream for image URI")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            uri
        }
    }

    /**
     * Saves the canvas state to a local File in the app's external files directory as a fallback.
     */
    fun saveToLocalFile(
        context: Context,
        sketchState: SketchCanvasState,
        pageNumber: Int,
        cameraDistance: String = "",
        separationForce: String = ""
    ): Result<File> {
        return runCatching {
            val formatted = pageNumber.toString().padStart(2, '0')
            val timestamp = System.currentTimeMillis()
            val fileName = "project_okuz_p${formatted}_sketch_$timestamp.png"

            val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: context.filesDir
            dir.mkdirs()
            val file = File(dir, fileName)

            FileOutputStream(file).use { out ->
                exportCanvasToStream(
                    sketchState = sketchState,
                    pageNumber = pageNumber,
                    cameraDistance = cameraDistance,
                    separationForce = separationForce,
                    outputStream = out
                )
            }
            file
        }
    }

    private fun drawStamp(
        canvas: Canvas,
        stamp: SketchStamp,
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
                    moveTo((260f + ox) * scaleX, (140f + oy) * scaleY)
                    lineTo((295f + ox) * scaleX, (195f + oy) * scaleY)
                    lineTo((310f + ox) * scaleX, (340f + oy) * scaleY)
                    lineTo((210f + ox) * scaleX, (340f + oy) * scaleY)
                    lineTo((225f + ox) * scaleX, (195f + oy) * scaleY)
                    close()
                }
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#F59E0B")
                paint.alpha = (45 * alpha).toInt()
                canvas.drawPath(path, paint)

                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#F59E0B")
                paint.alpha = (255 * alpha).toInt()
                paint.strokeWidth = 2.5f * scaleX
                canvas.drawPath(path, paint)
            }
            StampType.CHIWEENIE -> {
                val path = Path().apply {
                    moveTo((520f + ox) * scaleX, (315f + oy) * scaleY)
                    lineTo((530f + ox) * scaleX, (290f + oy) * scaleY)
                    lineTo((540f + ox) * scaleX, (310f + oy) * scaleY)
                    lineTo((550f + ox) * scaleX, (290f + oy) * scaleY)
                    lineTo((560f + ox) * scaleX, (315f + oy) * scaleY)
                    lineTo((595f + ox) * scaleX, (320f + oy) * scaleY)
                    lineTo((595f + ox) * scaleX, (340f + oy) * scaleY)
                    lineTo((520f + ox) * scaleX, (340f + oy) * scaleY)
                    close()
                }
                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#14B8A6")
                paint.alpha = (50 * alpha).toInt()
                canvas.drawPath(path, paint)

                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#14B8A6")
                paint.alpha = (255 * alpha).toInt()
                paint.strokeWidth = 2.5f * scaleX
                canvas.drawPath(path, paint)
            }
            StampType.OBSTRUCTION -> {
                val rx = (160f + ox) * scaleX
                val ry = (60f + oy) * scaleY
                val rw = 140f * scaleX
                val rh = 330f * scaleY

                paint.style = Paint.Style.FILL
                paint.color = Color.parseColor("#F43F5E")
                paint.alpha = (35 * alpha).toInt()
                canvas.drawRect(rx, ry, rx + rw, ry + rh, paint)

                paint.style = Paint.Style.STROKE
                paint.color = Color.parseColor("#F43F5E")
                paint.alpha = (255 * alpha).toInt()
                paint.strokeWidth = 2.2f * scaleX
                paint.pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
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
        } catch (_: Exception) {
            Color.parseColor("#F59E0B")
        }
    }
}
