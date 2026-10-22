package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.SketchCanvasState
import com.example.data.model.SketchLayer
import com.example.data.model.SketchPoint
import com.example.data.model.SketchStamp
import com.example.data.model.StampType
import com.example.ui.theme.BulkheadBorder
import com.example.ui.theme.BulkheadDivider
import com.example.ui.theme.CinzelFontFamily
import com.example.ui.theme.CommodoreAmber
import com.example.ui.theme.CommodoreAmberBright
import com.example.ui.theme.FireflyBrassBorder
import com.example.ui.theme.FireflyCopper
import com.example.ui.theme.HazardRose
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MutterPhosphorGreen
import com.example.ui.theme.NostromoPanelCard
import com.example.ui.theme.NostromoRecessedWell
import com.example.ui.theme.NostromoSlateSurface
import com.example.ui.theme.NostromoTeal
import com.example.ui.theme.NostromoTealBright
import com.example.ui.theme.NostromoVoidBg
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.example.ui.viewmodel.DrawTool

private const val VIRTUAL_WIDTH = 800f
private const val VIRTUAL_HEIGHT = 450f

val PHOSPHOR_COLOR_SWATCHES = listOf(
    "#F59E0B" to "Amber CRT",
    "#10B981" to "MU/TH/UR Green",
    "#14B8A6" to "Chiweenie Teal",
    "#EA580C" to "Serenity Copper",
    "#F43F5E" to "Hazard Rose",
    "#F8FAFC" to "Stark White"
)

fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val longVal = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(0xFF000000 or longVal)
        } else {
            Color(longVal)
        }
    } catch (e: Exception) {
        CommodoreAmber
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MultiLayerSketchPad(
    pageNumber: Int,
    cameraDistance: String,
    activeRuleLabels: List<String>,
    sketchState: SketchCanvasState,
    drawTool: DrawTool,
    penColorHex: String,
    onSetDrawTool: (DrawTool) -> Unit,
    onSetPenColor: (String) -> Unit,
    onAddStroke: (List<SketchPoint>) -> Unit,
    onStampMarker: (StampType) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit = {},
    canUndo: Boolean = true,
    canRedo: Boolean = false,
    undoCount: Int = 0,
    redoCount: Int = 0,
    onClearActiveLayer: () -> Unit,
    onSelectLayer: (String) -> Unit,
    onAddLayer: () -> Unit,
    onDeleteLayer: (String) -> Unit,
    onMoveLayerUp: (String) -> Unit,
    onMoveLayerDown: (String) -> Unit,
    onToggleLayerVisibility: (String) -> Unit,
    onRenameLayer: (String, String) -> Unit = { _, _ -> },
    onSetLayerOpacity: (String, Float) -> Unit = { _, _ -> },
    onMergeLayerDown: (String) -> Unit = {},
    onFlattenAllLayers: () -> Unit = {},
    onToggleSpreadMode: () -> Unit = {},
    onCombineTwoPagesAsOne: (Int, Boolean) -> Unit = { _, _ -> },
    onExportPng: () -> Unit = {},
    onResetEntirePage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activepoints = remember { mutableStateListOf<SketchPoint>() }
    var isCurrentlyDragging by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()
    val activeLayer = sketchState.activeLayer

    var showCombineSpreadDialog by remember { mutableStateOf(false) }
    var showRenameDialogForLayerId by remember { mutableStateOf<String?>(null) }
    var showAddLayerDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section Header with Spread Mode Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (sketchState.isSpreadMode) NostromoTealBright else CommodoreAmber, CircleShape)
                )
                Text(
                    text = if (sketchState.isSpreadMode) {
                        "THUMBNAIL SKETCH // TWO-PAGE SPREAD (32:9 CONTINUOUS SPINE)"
                    } else {
                        "THUMBNAIL SKETCH / CINEMATIC FRAMING (16:9 / SPREAD)"
                    },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (sketchState.isSpreadMode) NostromoTealBright else TerminalTextPrimary
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (sketchState.isSpreadMode) {
                    val facing = sketchState.combinedFacingPageNumber ?: (if (pageNumber % 2 == 0) pageNumber + 1 else pageNumber - 1).coerceIn(1, 25)
                    Surface(
                        color = NostromoTeal.copy(alpha = 0.20f),
                        border = BorderStroke(1.dp, NostromoTeal),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.clickable { showCombineSpreadDialog = true }
                    ) {
                        Text(
                            text = "SPREAD: P${pageNumber.toString().padStart(2, '0')} + P${facing.toString().padStart(2, '0')}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NostromoTealBright,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(
                    text = "ACTIVE LAYER: ${activeLayer?.name ?: "NONE"}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = if (activeLayer?.visible == true) CommodoreAmberBright else HazardRose
                )
            }
        }

        // Interactive Drawing Toolbar
        Surface(
            color = NostromoRecessedWell,
            shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp),
            border = BorderStroke(1.dp, BulkheadBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Row 1: Pen, Eraser, Color Swatches, Undo, Clear Layer, Two-Page Spread Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Pen Button
                    val isPen = drawTool == DrawTool.PEN
                    Surface(
                        color = if (isPen) CommodoreAmber else NostromoPanelCard,
                        border = BorderStroke(1.dp, if (isPen) CommodoreAmberBright else BulkheadBorder),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { onSetDrawTool(DrawTool.PEN) }
                            .testTag("tool_pen")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Pen Tool",
                                tint = if (isPen) NostromoVoidBg else TerminalTextPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "✎ Pen",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isPen) NostromoVoidBg else TerminalTextPrimary
                            )
                        }
                    }

                    // Eraser Button
                    val isEraser = drawTool == DrawTool.ERASER
                    Surface(
                        color = if (isEraser) CommodoreAmber else NostromoPanelCard,
                        border = BorderStroke(1.dp, if (isEraser) CommodoreAmberBright else BulkheadBorder),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { onSetDrawTool(DrawTool.ERASER) }
                            .testTag("tool_eraser")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⌫ Eraser",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isEraser) NostromoVoidBg else TerminalTextPrimary
                            )
                        }
                    }

                    // Color Swatches
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PHOSPHOR_COLOR_SWATCHES.forEach { (hex, _) ->
                            val swatchColor = parseHexColor(hex)
                            val isSelected = penColorHex.equals(hex, ignoreCase = true) && isPen
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else NostromoVoidBg,
                                        shape = CircleShape
                                    )
                                    .clickable { onSetPenColor(hex) }
                                    .testTag("swatch_${hex.removePrefix("#")}")
                            )
                        }
                    }

                    // Undo Button
                    Surface(
                        color = if (canUndo) NostromoPanelCard else NostromoPanelCard.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (canUndo) CommodoreAmber.copy(alpha = 0.65f) else BulkheadBorder.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable(enabled = canUndo) { onUndo() }
                            .testTag("sketch_undo")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo Drawing Action",
                                tint = if (canUndo) CommodoreAmberBright else TerminalTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (undoCount > 0) "Undo ($undoCount)" else "Undo",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = if (canUndo) FontWeight.Bold else FontWeight.Normal,
                                color = if (canUndo) TerminalTextPrimary else TerminalTextSecondary
                            )
                        }
                    }

                    // Redo Button
                    Surface(
                        color = if (canRedo) NostromoPanelCard else NostromoPanelCard.copy(alpha = 0.45f),
                        border = BorderStroke(
                            1.dp,
                            if (canRedo) MutterPhosphorGreen.copy(alpha = 0.65f) else BulkheadBorder.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable(enabled = canRedo) { onRedo() }
                            .testTag("sketch_redo")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo Drawing Action",
                                tint = if (canRedo) MutterPhosphorGreen else TerminalTextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (redoCount > 0) "Redo ($redoCount)" else "Redo",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = if (canRedo) FontWeight.Bold else FontWeight.Normal,
                                color = if (canRedo) TerminalTextPrimary else TerminalTextSecondary
                            )
                        }
                    }

                    // Two-Page Spread Button: Combine Two Pages as One
                    Surface(
                        color = if (sketchState.isSpreadMode) NostromoTeal.copy(alpha = 0.25f) else NostromoPanelCard,
                        border = BorderStroke(1.dp, if (sketchState.isSpreadMode) NostromoTealBright else BulkheadBorder),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { showCombineSpreadDialog = true }
                            .testTag("combine_two_pages_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = "Combine Two Pages as One Spread",
                                tint = if (sketchState.isSpreadMode) NostromoTealBright else CommodoreAmberBright,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (sketchState.isSpreadMode) "2-Page Spread: ON" else "Combine 2 Pages",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sketchState.isSpreadMode) NostromoTealBright else TerminalTextPrimary
                            )
                        }
                    }

                    // Save PNG Image Button
                    Surface(
                        color = NostromoPanelCard,
                        border = BorderStroke(1.dp, NostromoTeal.copy(alpha = 0.65f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { onExportPng() }
                            .testTag("export_png_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "Save Canvas as PNG Image",
                                tint = NostromoTealBright,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Save PNG",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerminalTextPrimary
                            )
                        }
                    }

                    // Clear Active Layer Button
                    Surface(
                        color = HazardRose.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, HazardRose.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { onClearActiveLayer() }
                            .testTag("clear_sketch_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear Active Layer",
                                tint = HazardRose,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Clear Sketch",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HazardRose
                            )
                        }
                    }
                }

                // Row 2: Template Stamp Markers
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    StampGuideButton(
                        label = "+ Hood Silhouette",
                        tint = CommodoreAmberBright,
                        tag = "stamp_hood",
                        onClick = { onStampMarker(StampType.HOOD) }
                    )
                    StampGuideButton(
                        label = "+ Chiweenie Scale",
                        tint = NostromoTeal,
                        tag = "stamp_chiweenie",
                        onClick = { onStampMarker(StampType.CHIWEENIE) }
                    )
                    StampGuideButton(
                        label = "+ Foreground Obstruction",
                        tint = HazardRose,
                        tag = "stamp_obstruction",
                        onClick = { onStampMarker(StampType.OBSTRUCTION) }
                    )
                }
            }
        }

        // 16:9 / 32:9 Interactive CRT Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .background(NostromoVoidBg)
                .border(
                    width = 1.5.dp,
                    color = when {
                        activeLayer?.visible == false -> HazardRose
                        sketchState.isSpreadMode -> NostromoTealBright
                        else -> FireflyBrassBorder
                    },
                    shape = RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp)
                )
                .clip(RoundedCornerShape(bottomStart = 10.dp, bottomEnd = 10.dp))
                .testTag("sketchpad_canvas")
                .pointerInput(sketchState.activeLayerId, drawTool, penColorHex, activeLayer?.visible) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (activeLayer?.visible != false) {
                                isCurrentlyDragging = true
                                activepoints.clear()
                                activepoints.add(
                                    SketchPoint(
                                        x = (offset.x / size.width) * VIRTUAL_WIDTH,
                                        y = (offset.y / size.height) * VIRTUAL_HEIGHT
                                    )
                                )
                            }
                        },
                        onDrag = { change, _ ->
                            if (isCurrentlyDragging) {
                                change.consume()
                                val pos = change.position
                                activepoints.add(
                                    SketchPoint(
                                        x = (pos.x.coerceIn(0f, size.width.toFloat()) / size.width) * VIRTUAL_WIDTH,
                                        y = (pos.y.coerceIn(0f, size.height.toFloat()) / size.height) * VIRTUAL_HEIGHT
                                    )
                                )
                            }
                        },
                        onDragEnd = {
                            if (isCurrentlyDragging && activepoints.isNotEmpty()) {
                                onAddStroke(activepoints.toList())
                            }
                            activepoints.clear()
                            isCurrentlyDragging = false
                        },
                        onDragCancel = {
                            activepoints.clear()
                            isCurrentlyDragging = false
                        }
                    )
                }
                .pointerInput(sketchState.activeLayerId, drawTool, penColorHex, activeLayer?.visible) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (activeLayer?.visible != false) {
                                val pt = SketchPoint(
                                    x = (offset.x / size.width) * VIRTUAL_WIDTH,
                                    y = (offset.y / size.height) * VIRTUAL_HEIGHT
                                )
                                onAddStroke(listOf(pt, SketchPoint(pt.x + 1.5f, pt.y + 1.5f)))
                            }
                        }
                    )
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val scaleX = size.width / VIRTUAL_WIDTH
                val scaleY = size.height / VIRTUAL_HEIGHT

                // 1. Safe Frame Wireframe Guide
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(40f * scaleX, 30f * scaleY),
                    size = Size(720f * scaleX, 390f * scaleY),
                    cornerRadius = CornerRadius(8f * scaleX, 8f * scaleY),
                    style = Stroke(
                        width = 1.5f * scaleX,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f * scaleX, 6f * scaleX))
                    )
                )

                // 2. TWO-PAGE SPREAD: Center Book Spine / Gutter Divider & Page Halves
                if (sketchState.isSpreadMode) {
                    val spineX = (VIRTUAL_WIDTH / 2f) * scaleX
                    // Spine shadow / book fold crease gradient
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0x33000000),
                                Color(0x77000000),
                                Color(0x33000000),
                                Color.Transparent
                            ),
                            startX = spineX - 22f * scaleX,
                            endX = spineX + 22f * scaleX
                        ),
                        topLeft = Offset(spineX - 22f * scaleX, 0f),
                        size = Size(44f * scaleX, size.height)
                    )
                    // Spine center dashed guide line
                    drawLine(
                        color = CommodoreAmberBright,
                        start = Offset(spineX, 0f),
                        end = Offset(spineX, size.height),
                        strokeWidth = 2f * scaleX,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * scaleX, 6f * scaleX))
                    )

                    // Page spine labels
                    val leftLabel = "PAGE ${pageNumber.toString().padStart(2, '0')} // LEFT"
                    val leftMeasured = textMeasurer.measure(
                        text = leftLabel,
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CommodoreAmberBright.copy(alpha = 0.75f)
                        )
                    )
                    drawText(
                        textLayoutResult = leftMeasured,
                        topLeft = Offset(50f * scaleX, 38f * scaleY)
                    )

                    val facing = sketchState.combinedFacingPageNumber
                        ?: (if (pageNumber % 2 == 0) pageNumber + 1 else pageNumber - 1).coerceIn(1, 25)
                    val rightLabel = "PAGE ${facing.toString().padStart(2, '0')} // RIGHT"
                    val rightMeasured = textMeasurer.measure(
                        text = rightLabel,
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NostromoTealBright.copy(alpha = 0.75f)
                        )
                    )
                    drawText(
                        textLayoutResult = rightMeasured,
                        topLeft = Offset(size.width - rightMeasured.size.width - 50f * scaleX, 38f * scaleY)
                    )
                } else {
                    // Standard Rule of Thirds
                    for (i in 1..2) {
                        val thirdX = size.width * (i / 3f)
                        val thirdY = size.height * (i / 3f)
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(thirdX, 0f),
                            end = Offset(thirdX, size.height),
                            strokeWidth = 1f
                        )
                        drawLine(
                            color = Color(0xFF1E293B),
                            start = Offset(0f, thirdY),
                            end = Offset(size.width, thirdY),
                            strokeWidth = 1f
                        )
                    }
                }

                // Template Framing Watermark when no visible strokes exist
                val anyVisibleElements = sketchState.layers.any { it.visible && it.elementCount > 0 }
                if (!anyVisibleElements && activepoints.isEmpty()) {
                    val pageTag = if (sketchState.isSpreadMode) {
                        val facing = sketchState.combinedFacingPageNumber ?: (pageNumber + 1)
                        "[ COMBINED SPREAD: PAGES ${pageNumber.toString().padStart(2, '0')} & ${facing.toString().padStart(2, '0')} // READY ]"
                    } else {
                        "[ PAGE ${pageNumber.toString().padStart(2, '0')} OF 25 // READY ]"
                    }
                    val measured = textMeasurer.measure(
                        text = pageTag,
                        style = TextStyle(
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = (if (sketchState.isSpreadMode) NostromoTeal else CommodoreAmber).copy(alpha = 0.55f)
                        )
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(
                            (size.width - measured.size.width) / 2f,
                            (size.height - measured.size.height) / 2f
                        )
                    )
                }

                // 3. Render Visible Layers in Stack Order (bottom to top), respecting layer opacity
                sketchState.layers.forEach { layer ->
                    if (layer.visible) {
                        val alpha = layer.opacity
                        // Draw Stamps on this layer
                        layer.stamps.forEach { stamp ->
                            drawStampGuide(stamp, scaleX, scaleY, alpha)
                        }
                        // Draw Strokes on this layer
                        layer.strokes.forEach { stroke ->
                            val strokeColor = parseHexColor(stroke.colorHex).copy(alpha = alpha)
                            drawSketchPoints(
                                points = stroke.points,
                                color = strokeColor,
                                strokeWidth = stroke.strokeWidth * scaleX,
                                scaleX = scaleX,
                                scaleY = scaleY
                            )
                        }
                    }
                }

                // 4. Render Current Live Stroke on Active Layer
                if (activepoints.isNotEmpty() && drawTool == DrawTool.PEN) {
                    val liveAlpha = activeLayer?.opacity ?: 1.0f
                    drawSketchPoints(
                        points = activepoints,
                        color = parseHexColor(penColorHex).copy(alpha = liveAlpha),
                        strokeWidth = 3.5f * scaleX,
                        scaleX = scaleX,
                        scaleY = scaleY
                    )
                }
            }

            // Top-Left HUD Camera Stamp
            Surface(
                color = NostromoSlateSurface.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, BulkheadBorder),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                val spreadHUD = if (sketchState.isSpreadMode) {
                    val facing = sketchState.combinedFacingPageNumber ?: (pageNumber + 1)
                    "2-PAGE SPREAD (P${pageNumber.toString().padStart(2, '0')}+P${facing.toString().padStart(2, '0')}) | "
                } else {
                    "16:9 / SPREAD | "
                }
                Text(
                    text = "$spreadHUD CAMERA: ${if (cameraDistance.isNotBlank()) cameraDistance.uppercase() else "UNSELECTED"}",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = if (sketchState.isSpreadMode) NostromoTealBright else CommodoreAmberBright,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            // Hidden Layer Warning Overlay if active layer is hidden
            if (activeLayer?.visible == false) {
                Surface(
                    color = HazardRose.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(12.dp)
                ) {
                    Text(
                        text = "SELECTED LAYER IS HIDDEN — TOGGLE VISIBILITY (EYE ICON) TO DRAW",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Bottom-Right HUD Main Character Rule Stamp
            Surface(
                color = NostromoSlateSurface.copy(alpha = 0.90f),
                border = BorderStroke(1.dp, BulkheadBorder),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                val ruleSummary = if (activeRuleLabels.isNotEmpty()) {
                    activeRuleLabels.joinToString(" • ")
                } else {
                    "NONE CHECKED"
                }
                Text(
                    text = "MAIN CHARACTER RULE: $ruleSummary",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = if (activeRuleLabels.isNotEmpty()) MutterPhosphorGreen else CommodoreAmberBright,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // DEDICATED LAYER MANAGEMENT UI COMPONENT
        SketchLayerDeckPanel(
            layers = sketchState.layers,
            activeLayerId = sketchState.activeLayerId,
            isSpreadMode = sketchState.isSpreadMode,
            currentPageNumber = pageNumber,
            combinedFacingPage = sketchState.combinedFacingPageNumber,
            onSelectLayer = onSelectLayer,
            onAddLayerClick = { showAddLayerDialog = true },
            onDeleteLayer = onDeleteLayer,
            onMoveLayerUp = onMoveLayerUp,
            onMoveLayerDown = onMoveLayerDown,
            onToggleLayerVisibility = onToggleLayerVisibility,
            onRenameClick = { layerId -> showRenameDialogForLayerId = layerId },
            onSetLayerOpacity = onSetLayerOpacity,
            onMergeLayerDown = onMergeLayerDown,
            onFlattenAll = onFlattenAllLayers,
            onOpenSpreadDialog = { showCombineSpreadDialog = true }
        )

        // Caption + Reset Entire Page Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Draw across layers • Combine 2 pages as one continuous 32:9 spread",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = TerminalTextSecondary
            )
            Text(
                text = "Reset Entire Page ${pageNumber.toString().padStart(2, '0')}",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = HazardRose,
                modifier = Modifier
                    .clickable { onResetEntirePage() }
                    .padding(vertical = 4.dp, horizontal = 6.dp)
                    .testTag("reset_page_button")
            )
        }
    }

    // DIALOG: COMBINE TWO PAGES AS ONE (SPREAD MODE)
    if (showCombineSpreadDialog) {
        CombineTwoPagesDialog(
            currentPageNumber = pageNumber,
            currentFacingPage = sketchState.combinedFacingPageNumber ?: (if (pageNumber % 2 == 0) pageNumber + 1 else pageNumber - 1).coerceIn(1, 25),
            isSpreadActive = sketchState.isSpreadMode,
            onDismiss = { showCombineSpreadDialog = false },
            onToggleSpreadGutter = {
                onToggleSpreadMode()
                showCombineSpreadDialog = false
            },
            onCombinePagesAsOne = { targetFacing, mergeManuscript ->
                onCombineTwoPagesAsOne(targetFacing, mergeManuscript)
                showCombineSpreadDialog = false
            }
        )
    }

    // DIALOG: ADD LAYER (WITH PRESETS OR CUSTOM NAME)
    if (showAddLayerDialog) {
        AddLayerDialog(
            nextLayerNumber = sketchState.layers.size + 1,
            onDismiss = { showAddLayerDialog = false },
            onAddLayer = { chosenName ->
                onAddLayer()
                val targetLayer = sketchState.layers.lastOrNull()
                if (targetLayer != null && chosenName.isNotBlank()) {
                    onRenameLayer(targetLayer.id, chosenName)
                }
                showAddLayerDialog = false
            }
        )
    }

    // DIALOG: RENAME LAYER
    if (showRenameDialogForLayerId != null) {
        val targetLayerId = showRenameDialogForLayerId!!
        val currentLayer = sketchState.layers.firstOrNull { it.id == targetLayerId }
        if (currentLayer != null) {
            RenameLayerDialog(
                currentName = currentLayer.name,
                onDismiss = { showRenameDialogForLayerId = null },
                onSave = { newName ->
                    onRenameLayer(targetLayerId, newName)
                    showRenameDialogForLayerId = null
                }
            )
        }
    }
}

@Composable
private fun StampGuideButton(
    label: String,
    tint: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        color = NostromoPanelCard,
        border = BorderStroke(1.dp, tint.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

/**
 * Enhanced Layer Management UI Component:
 * Allows users to add, delete, reorder, toggle visibility, adjust opacity,
 * merge layers into one, and combine two pages as one spread.
 */
@Composable
fun SketchLayerDeckPanel(
    layers: List<SketchLayer>,
    activeLayerId: String,
    isSpreadMode: Boolean,
    currentPageNumber: Int,
    combinedFacingPage: Int?,
    onSelectLayer: (String) -> Unit,
    onAddLayerClick: () -> Unit,
    onDeleteLayer: (String) -> Unit,
    onMoveLayerUp: (String) -> Unit,
    onMoveLayerDown: (String) -> Unit,
    onToggleLayerVisibility: (String) -> Unit,
    onRenameClick: (String) -> Unit,
    onSetLayerOpacity: (String, Float) -> Unit,
    onMergeLayerDown: (String) -> Unit,
    onFlattenAll: () -> Unit,
    onOpenSpreadDialog: () -> Unit
) {
    Surface(
        color = NostromoRecessedWell,
        border = BorderStroke(1.dp, if (isSpreadMode) NostromoTeal.copy(alpha = 0.6f) else BulkheadBorder),
        shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sketch_layer_deck")
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // LAYER DECK HEADER & GLOBAL ACTIONS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Sketchpad Layers",
                        tint = if (isSpreadMode) NostromoTealBright else CommodoreAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "SKETCHPAD LAYER BUS (${layers.size} LAYERS)",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSpreadMode) NostromoTealBright else CommodoreAmber
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Combine Two Pages as One Spread button
                    Surface(
                        color = if (isSpreadMode) NostromoTeal.copy(alpha = 0.22f) else NostromoPanelCard,
                        border = BorderStroke(1.dp, if (isSpreadMode) NostromoTealBright else FireflyBrassBorder),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable { onOpenSpreadDialog() }
                            .testTag("open_spread_panel_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoStories,
                                contentDescription = "Combine Two Pages as One",
                                tint = if (isSpreadMode) NostromoTealBright else CommodoreAmberBright,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (isSpreadMode) "SPREAD ACTIVE" else "2-PAGE SPREAD",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSpreadMode) NostromoTealBright else CommodoreAmberBright
                            )
                        }
                    }

                    // Flatten All Layers button
                    if (layers.size > 1) {
                        Surface(
                            color = NostromoPanelCard,
                            border = BorderStroke(1.dp, BulkheadBorder),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clickable { onFlattenAll() }
                                .testTag("flatten_all_layers_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LayersClear,
                                    contentDescription = "Flatten Layers",
                                    tint = TerminalTextSecondary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = "FLATTEN",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TerminalTextSecondary
                                )
                            }
                        }
                    }

                    // Add Layer button
                    Surface(
                        color = CommodoreAmber.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, CommodoreAmber),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .clickable { onAddLayerClick() }
                            .testTag("add_layer_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Layer",
                                tint = CommodoreAmberBright,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "ADD LAYER",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CommodoreAmberBright
                            )
                        }
                    }
                }
            }

            // Two-page spread status banner if enabled
            if (isSpreadMode) {
                val facing = combinedFacingPage ?: (if (currentPageNumber % 2 == 0) currentPageNumber + 1 else currentPageNumber - 1).coerceIn(1, 25)
                Surface(
                    color = NostromoTeal.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, NostromoTeal.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2-PAGE SPREAD COMBINED: PAGE ${currentPageNumber.toString().padStart(2, '0')} + PAGE ${facing.toString().padStart(2, '0')} (GUTTER SPINE ACTIVE)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NostromoTealBright
                        )
                        Text(
                            text = "EDIT SPREAD",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = CommodoreAmberBright,
                            modifier = Modifier.clickable { onOpenSpreadDialog() }
                        )
                    }
                }
            }

            // INDIVIDUAL LAYER ROWS
            layers.forEachIndexed { index, layer ->
                val isSelected = layer.id == activeLayerId
                Surface(
                    color = if (isSelected) NostromoPanelCard else NostromoSlateSurface,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = when {
                            isSelected && isSpreadMode -> NostromoTealBright
                            isSelected -> CommodoreAmber
                            else -> BulkheadBorder
                        }
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLayer(layer.id) }
                        .testTag("layer_row_$index")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Visibility toggle + Name + Active badge + Stats
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = { onToggleLayerVisibility(layer.id) },
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("layer_visibility_$index")
                            ) {
                                Icon(
                                    imageVector = if (layer.visible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (layer.visible) "Hide Layer" else "Show Layer",
                                    tint = if (layer.visible) MutterPhosphorGreen else TerminalTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = layer.name,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (layer.visible) TerminalTextPrimary else TerminalTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    // Rename button
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename Layer",
                                        tint = TerminalTextSecondary.copy(alpha = 0.7f),
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clickable { onRenameClick(layer.id) }
                                            .testTag("rename_layer_$index")
                                    )
                                    if (isSelected) {
                                        Surface(
                                            color = if (isSpreadMode) NostromoTeal else CommodoreAmber,
                                            shape = RoundedCornerShape(3.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NostromoVoidBg,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "${layer.strokes.size} strokes • ${layer.stamps.size} guides",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 9.sp,
                                        color = TerminalTextSecondary
                                    )
                                    // Opacity cycle tag
                                    val opacityPercent = (layer.opacity * 100).toInt()
                                    Surface(
                                        color = NostromoRecessedWell,
                                        shape = RoundedCornerShape(3.dp),
                                        modifier = Modifier
                                            .clickable {
                                                val nextOpacity = when {
                                                    layer.opacity > 0.85f -> 0.75f
                                                    layer.opacity > 0.6f -> 0.50f
                                                    layer.opacity > 0.35f -> 0.25f
                                                    else -> 1.0f
                                                }
                                                onSetLayerOpacity(layer.id, nextOpacity)
                                            }
                                            .testTag("layer_opacity_$index")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Opacity,
                                                contentDescription = "Layer Opacity",
                                                tint = TerminalTextSecondary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "$opacityPercent%",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 8.sp,
                                                color = TerminalTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Right: Reorder Up, Reorder Down, Merge Down, Delete
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Merge down button (available if not bottom layer)
                            if (index < layers.lastIndex) {
                                IconButton(
                                    onClick = { onMergeLayerDown(layer.id) },
                                    modifier = Modifier
                                        .size(30.dp)
                                        .testTag("layer_merge_down_$index")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallMerge,
                                        contentDescription = "Merge Layer Down",
                                        tint = CommodoreAmberBright,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onMoveLayerUp(layer.id) },
                                enabled = index > 0,
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("layer_up_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Move Layer Up",
                                    tint = if (index > 0) TerminalTextPrimary else TerminalTextSecondary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { onMoveLayerDown(layer.id) },
                                enabled = index < layers.lastIndex,
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("layer_down_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDownward,
                                    contentDescription = "Move Layer Down",
                                    tint = if (index < layers.lastIndex) TerminalTextPrimary else TerminalTextSecondary.copy(alpha = 0.3f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            IconButton(
                                onClick = { onDeleteLayer(layer.id) },
                                modifier = Modifier
                                    .size(30.dp)
                                    .testTag("layer_delete_$index")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Layer",
                                    tint = HazardRose.copy(alpha = 0.85f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog allowing users to combine two facing pages into one continuous spread.
 */
@Composable
fun CombineTwoPagesDialog(
    currentPageNumber: Int,
    currentFacingPage: Int,
    isSpreadActive: Boolean,
    onDismiss: () -> Unit,
    onToggleSpreadGutter: () -> Unit,
    onCombinePagesAsOne: (Int, Boolean) -> Unit
) {
    var selectedFacingPage by remember { mutableIntStateOf(currentFacingPage) }
    var mergeManuscript by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = NostromoSlateSurface,
            shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
            border = BorderStroke(1.5.dp, NostromoTealBright),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = "Combine Two Pages",
                            tint = NostromoTealBright,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "COMBINE TWO PAGES AS ONE",
                            fontFamily = CinzelFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TerminalTextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TerminalTextSecondary)
                    }
                }

                Text(
                    text = "In picture book & storyboard production, a 2-page spread combines facing pages into a continuous panoramic scene across the book spine.",
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    color = TerminalTextSecondary
                )

                HorizontalDivider(color = BulkheadDivider)

                // Select Facing Page
                Text(
                    text = "SELECT FACING PAGE TO COMBINE WITH PAGE ${currentPageNumber.toString().padStart(2, '0')}:",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CommodoreAmber
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val prevPage = (currentPageNumber - 1).coerceAtLeast(1)
                    val nextPage = (currentPageNumber + 1).coerceAtMost(25)

                    if (prevPage != currentPageNumber) {
                        val isPrevSelected = selectedFacingPage == prevPage
                        Surface(
                            color = if (isPrevSelected) NostromoTeal.copy(alpha = 0.25f) else NostromoRecessedWell,
                            border = BorderStroke(1.dp, if (isPrevSelected) NostromoTealBright else BulkheadBorder),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFacingPage = prevPage }
                                .testTag("select_prev_spread_page")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "PREV PAGE",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = TerminalTextSecondary
                                )
                                Text(
                                    text = "Page ${prevPage.toString().padStart(2, '0')}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPrevSelected) NostromoTealBright else TerminalTextPrimary
                                )
                            }
                        }
                    }

                    if (nextPage != currentPageNumber) {
                        val isNextSelected = selectedFacingPage == nextPage
                        Surface(
                            color = if (isNextSelected) NostromoTeal.copy(alpha = 0.25f) else NostromoRecessedWell,
                            border = BorderStroke(1.dp, if (isNextSelected) NostromoTealBright else BulkheadBorder),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedFacingPage = nextPage }
                                .testTag("select_next_spread_page")
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "NEXT PAGE",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    color = TerminalTextSecondary
                                )
                                Text(
                                    text = "Page ${nextPage.toString().padStart(2, '0')}",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isNextSelected) NostromoTealBright else TerminalTextPrimary
                                )
                            }
                        }
                    }
                }

                // Options: Merge Manuscript
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { mergeManuscript = !mergeManuscript },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Checkbox(
                        checked = mergeManuscript,
                        onCheckedChange = { mergeManuscript = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = NostromoTealBright,
                            uncheckedColor = TerminalTextSecondary
                        )
                    )
                    Text(
                        text = "Combine storyline & illustration manuscript from Page ${selectedFacingPage.toString().padStart(2, '0')}",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = TerminalTextPrimary
                    )
                }

                HorizontalDivider(color = BulkheadDivider)

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Toggle Gutter Spine button
                    Surface(
                        color = if (isSpreadActive) HazardRose.copy(alpha = 0.15f) else NostromoPanelCard,
                        border = BorderStroke(1.dp, if (isSpreadActive) HazardRose else BulkheadBorder),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onToggleSpreadGutter() }
                            .testTag("toggle_spread_gutter_btn")
                    ) {
                        Text(
                            text = if (isSpreadActive) "DISABLE SPREAD VIEW" else "TOGGLE SPREAD GUTTER",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSpreadActive) HazardRose else TerminalTextPrimary,
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
                        )
                    }

                    // Full Combine button
                    Button(
                        onClick = { onCombinePagesAsOne(selectedFacingPage, mergeManuscript) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NostromoTealBright,
                            contentColor = NostromoVoidBg
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("confirm_combine_spread_btn")
                    ) {
                        Text(
                            text = "COMBINE AS ONE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog to add a layer with presets or custom name.
 */
@Composable
fun AddLayerDialog(
    nextLayerNumber: Int,
    onDismiss: () -> Unit,
    onAddLayer: (String) -> Unit
) {
    var layerName by remember { mutableStateOf("L$nextLayerNumber // OVERLAY") }
    val presets = listOf(
        "L$nextLayerNumber // TERRAIN & ENVIRONMENT",
        "L$nextLayerNumber // CHARACTER SILHOUETTES",
        "L$nextLayerNumber // FOREGROUND OBSTRUCTION",
        "L$nextLayerNumber // ATMOSPHERE & WEATHER FX",
        "L$nextLayerNumber // SPREAD GUTTER NOTES"
    )

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = NostromoSlateSurface,
            shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
            border = BorderStroke(1.5.dp, CommodoreAmber),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ADD SKETCHPAD DRAWING LAYER",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CommodoreAmber
                )

                OutlinedTextField(
                    value = layerName,
                    onValueChange = { layerName = it },
                    label = { Text("Layer Name", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp) },
                    textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = TerminalTextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CommodoreAmber,
                        unfocusedBorderColor = BulkheadBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_layer_name_input")
                )

                Text(
                    text = "PRESET LAYER TEMPLATES:",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 9.sp,
                    color = TerminalTextSecondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    presets.forEach { preset ->
                        Surface(
                            color = NostromoRecessedWell,
                            border = BorderStroke(1.dp, BulkheadBorder),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { layerName = preset }
                        ) {
                            Text(
                                text = preset,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = if (layerName == preset) CommodoreAmberBright else TerminalTextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NostromoPanelCard, contentColor = TerminalTextSecondary),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onAddLayer(layerName) },
                        colors = ButtonDefaults.buttonColors(containerColor = CommodoreAmber, contentColor = NostromoVoidBg),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("confirm_add_layer_btn")
                    ) {
                        Text("Add Layer", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Dialog to rename an existing layer.
 */
@Composable
fun RenameLayerDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var layerName by remember { mutableStateOf(currentName) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = NostromoSlateSurface,
            shape = CutCornerShape(topStart = 8.dp, bottomEnd = 8.dp),
            border = BorderStroke(1.5.dp, CommodoreAmber),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "RENAME LAYER",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CommodoreAmber
                )

                OutlinedTextField(
                    value = layerName,
                    onValueChange = { layerName = it },
                    label = { Text("Layer Name", fontFamily = JetBrainsMonoFontFamily, fontSize = 10.sp) },
                    textStyle = TextStyle(fontFamily = JetBrainsMonoFontFamily, fontSize = 12.sp, color = TerminalTextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CommodoreAmber,
                        unfocusedBorderColor = BulkheadBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rename_layer_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = NostromoPanelCard, contentColor = TerminalTextSecondary),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancel", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp)
                    }
                    Button(
                        onClick = { onSave(layerName) },
                        colors = ButtonDefaults.buttonColors(containerColor = CommodoreAmber, contentColor = NostromoVoidBg),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("confirm_rename_layer_btn")
                    ) {
                        Text("Save", fontFamily = JetBrainsMonoFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

fun DrawScope.drawSketchPoints(
    points: List<SketchPoint>,
    color: Color,
    strokeWidth: Float,
    scaleX: Float,
    scaleY: Float
) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        val pt = points.first()
        drawCircle(
            color = color,
            radius = strokeWidth / 2f,
            center = Offset(pt.x * scaleX, pt.y * scaleY)
        )
        return
    }
    val path = Path().apply {
        val first = points.first()
        moveTo(first.x * scaleX, first.y * scaleY)
        for (i in 1 until points.size) {
            val pt = points[i]
            lineTo(pt.x * scaleX, pt.y * scaleY)
        }
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(
            width = strokeWidth,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
    )
}

fun DrawScope.drawStampGuide(
    stamp: SketchStamp,
    scaleX: Float,
    scaleY: Float,
    alpha: Float = 1.0f
) {
    val ox = stamp.offsetX
    val oy = stamp.offsetY
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
            drawPath(path, color = CommodoreAmber.copy(alpha = 0.16f * alpha))
            drawPath(path, color = CommodoreAmber.copy(alpha = alpha), style = Stroke(width = 2.5f * scaleX))
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
            drawPath(path, color = NostromoTeal.copy(alpha = 0.18f * alpha))
            drawPath(path, color = NostromoTeal.copy(alpha = alpha), style = Stroke(width = 2.2f * scaleX))
        }
        StampType.OBSTRUCTION -> {
            val topLeft = Offset((160f + ox) * scaleX, (60f + oy) * scaleY)
            val rectSize = Size(140f * scaleX, 330f * scaleY)
            drawRect(
                color = HazardRose.copy(alpha = 0.12f * alpha),
                topLeft = topLeft,
                size = rectSize
            )
            drawRect(
                color = HazardRose.copy(alpha = alpha),
                topLeft = topLeft,
                size = rectSize,
                style = Stroke(
                    width = 2f * scaleX,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f * scaleX, 4f * scaleX))
                )
            )
        }
    }
}
