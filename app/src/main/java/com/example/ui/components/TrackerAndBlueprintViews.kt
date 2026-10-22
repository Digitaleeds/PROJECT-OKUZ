package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BOOK_ONE_PHASES
import com.example.data.model.EQ_FIVE_YEAR_PILLARS
import com.example.data.model.SketchCanvasState
import com.example.data.model.StoryboardPage
import com.example.data.model.getPhaseForPage
import com.example.ui.theme.BulkheadBorder
import com.example.ui.theme.BulkheadDivider
import com.example.ui.theme.CinzelFontFamily
import com.example.ui.theme.CommodoreAmber
import com.example.ui.theme.CommodoreAmberBright
import com.example.ui.theme.FireflyBrassBorder
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

@Composable
fun PhaseTrackerPanel(
    pagesMap: Map<Int, StoryboardPage>,
    currentPageNumber: Int,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedCount = pagesMap.values.count { it.completed }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NostromoSlateSurface,
        shape = CutCornerShape(topStart = 12.dp, bottomEnd = 12.dp, topEnd = 4.dp, bottomStart = 4.dp),
        border = BorderStroke(1.dp, BulkheadBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tracker Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NostromoVoidBg)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "2. BOOK ONE: 25-PAGE PACING & PRODUCTION TRACKER",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = CommodoreAmber
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Check off each page as storyboards are completed",
                        fontFamily = InterFontFamily,
                        fontSize = 11.sp,
                        color = TerminalTextSecondary
                    )
                    Text(
                        text = "$completedCount / 25",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CommodoreAmberBright,
                        modifier = Modifier.testTag("completion_counter")
                    )
                }
            }

            HorizontalDivider(color = BulkheadDivider)

            // 5 Phase Blocks
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                BOOK_ONE_PHASES.forEach { phase ->
                    Surface(
                        color = NostromoRecessedWell,
                        border = BorderStroke(1.dp, BulkheadBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = phase.code.uppercase(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CommodoreAmber
                            )
                            Text(
                                text = phase.title,
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerminalTextPrimary
                            )
                            Text(
                                text = phase.focus,
                                fontFamily = InterFontFamily,
                                fontSize = 11.sp,
                                color = TerminalTextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (p in phase.startPage..phase.endPage) {
                                    val isCurrent = p == currentPageNumber
                                    val isDone = pagesMap[p]?.completed == true
                                    val bgColor = when {
                                        isCurrent -> CommodoreAmber
                                        isDone -> MutterPhosphorGreen.copy(alpha = 0.16f)
                                        else -> NostromoSlateSurface
                                    }
                                    val borderColor = when {
                                        isCurrent -> CommodoreAmberBright
                                        isDone -> MutterPhosphorGreen.copy(alpha = 0.55f)
                                        else -> BulkheadBorder
                                    }
                                    val textColor = when {
                                        isCurrent -> NostromoVoidBg
                                        isDone -> MutterPhosphorGreen
                                        else -> TerminalTextSecondary
                                    }
                                    Surface(
                                        color = bgColor,
                                        border = BorderStroke(1.dp, borderColor),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onSelectPage(p) }
                                            .testTag("tracker_page_btn_$p")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = p.toString(),
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 12.sp,
                                                fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.Normal,
                                                color = textColor
                                            )
                                            Text(
                                                text = if (isDone) "☑" else "☐",
                                                fontFamily = JetBrainsMonoFontFamily,
                                                fontSize = 9.sp,
                                                color = textColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = BulkheadDivider)

            // Mandatory Visual Directive Reminder Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NostromoVoidBg)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Mandatory Visual Directive",
                        tint = HazardRose,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "MANDATORY VISUAL DIRECTIVE: THE MAIN CHARACTER RULE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HazardRose
                    )
                }
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TerminalTextPrimary)) {
                            append("Permanent Face Obscuration: ")
                        }
                        append(
                            "The hood-wearing 10-year-old protagonist never shows their face in any drawing. Through deliberate illustrative staging, the child's face is permanently hidden in every single panel—framed from behind, cast under deep hood shadow, eclipsed by massive tree trunks, or blocked by foreground environmental objects. Illustrative Mandate: Unknown / TBD."
                        )
                    },
                    fontFamily = InterFontFamily,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = TerminalTextSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasterBlueprintView(
    pagesMap: Map<Int, StoryboardPage>,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("blueprint_view"),
        color = NostromoSlateSurface,
        shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp),
        border = BorderStroke(1.5.dp, FireflyBrassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MASTER SERIES BLUEPRINT & PRODUCTION BIBLE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CommodoreAmber
                    )
                    Text(
                        text = "PROJECT OKUZ",
                        fontFamily = CinzelFontFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalTextPrimary
                    )
                }

                Surface(
                    color = NostromoRecessedWell,
                    border = BorderStroke(1.dp, BulkheadBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Series Cadence: 5 Books (1 / Yr)",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = TerminalTextPrimary
                        )
                        Text(
                            text = "Book 1 Theme: EQ Pillar I - Self-Regulation",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = CommodoreAmberBright
                        )
                        Text(
                            text = "Format: 25-Page Handwritten Storyboard",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = TerminalTextSecondary
                        )
                    }
                }
            }

            HorizontalDivider(color = BulkheadDivider)

            // 5-Book Annual Architecture
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "5-BOOK ANNUAL EMOTIONAL INTELLIGENCE ARCHITECTURE",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NostromoTealBright
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EQ_FIVE_YEAR_PILLARS.forEach { pillar ->
                        Surface(
                            color = if (pillar.isActiveBook) CommodoreAmber.copy(alpha = 0.16f) else NostromoRecessedWell,
                            border = BorderStroke(
                                1.dp,
                                if (pillar.isActiveBook) CommodoreAmber else BulkheadBorder
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = pillar.yearLabel,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pillar.isActiveBook) CommodoreAmberBright else TerminalTextSecondary
                                )
                                Text(
                                    text = pillar.pillarTitle,
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pillar.isActiveBook) CommodoreAmberBright else TerminalTextPrimary
                                )
                                Text(
                                    text = pillar.subtitle,
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    color = TerminalTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 1. Narrative Architecture & Premise
            Surface(
                color = NostromoRecessedWell,
                border = BorderStroke(1.dp, BulkheadBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "1. NARRATIVE ARCHITECTURE & BOOK ONE PREMISE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CommodoreAmber
                    )
                    Text(
                        text = "Working Title: Project Okuz",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TerminalTextSecondary
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TerminalTextPrimary)) {
                                append("Deeply Rooted Childhood Lifeline: ")
                            }
                            append(
                                "Project Okuz is engineered around the five major components required to become an emotionally intelligent individual, structuring a 10-year-old protagonist's developmental arc across five annual releases. Book One focuses on Self-Regulation—capturing the gripping lifeline of a 10-year-old and a tiny, pointy-eared Chiweenie who have just met. Though newly bonded, harsh nature, bad luck, and fate constantly tear them apart. Driven by dramatic-cinematic pacing and high-stakes adventure-mystery across unforgiving environments, the narrative tracks their relentless fight against all odds to reunite and protect their fragile new friendship."
                            )
                        },
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TerminalTextSecondary
                    )
                }
            }

            // Mandatory Visual Directive Card
            Surface(
                color = HazardRose.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, HazardRose.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "MANDATORY VISUAL DIRECTIVE: THE MAIN CHARACTER RULE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HazardRose
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TerminalTextPrimary)) {
                                append("Permanent Face Obscuration: ")
                            }
                            append(
                                "The hood-wearing 10-year-old protagonist never shows their face in any drawing. Through deliberate illustrative staging, the child's face is permanently hidden in every single panel—framed from behind, cast under deep hood shadow, eclipsed by massive tree trunks, or blocked by foreground environmental objects. Illustrative Mandate: Unknown / TBD."
                            )
                        },
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TerminalTextSecondary
                    )
                }
            }

            // 2. Book One 25-Page Pacing Table
            Surface(
                color = NostromoRecessedWell,
                border = BorderStroke(1.dp, BulkheadBorder),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "2. BOOK ONE: 25-PAGE PACING & PRODUCTION TRACKER",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CommodoreAmber
                        )
                    }
                    HorizontalDivider(color = BulkheadDivider)

                    BOOK_ONE_PHASES.forEachIndexed { index, phase ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = phase.code.uppercase(),
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CommodoreAmberBright
                            )
                            Text(
                                text = phase.title,
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerminalTextPrimary
                            )
                            Text(
                                text = phase.focus,
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = TerminalTextSecondary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (p in phase.startPage..phase.endPage) {
                                    val done = pagesMap[p]?.completed == true
                                    Surface(
                                        color = if (done) MutterPhosphorGreen.copy(alpha = 0.2f) else NostromoSlateSurface,
                                        border = BorderStroke(
                                            1.dp,
                                            if (done) MutterPhosphorGreen else BulkheadBorder
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        modifier = Modifier
                                            .clickable { onSelectPage(p) }
                                            .testTag("blueprint_page_btn_$p")
                                    ) {
                                        Text(
                                            text = "${if (done) "☑" else "☐"} $p",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (done) MutterPhosphorGreen else TerminalTextPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (index < BOOK_ONE_PHASES.lastIndex) {
                            HorizontalDivider(color = BulkheadDivider)
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "PROJECT OKUZ - Master Series Blueprint (Book 1: Self-Regulation)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = TerminalTextSecondary
                )
                Text(
                    text = "Page 1 of 2 Series Reference Sheet",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = TerminalTextSecondary
                )
            }
        }
    }
}

@Composable
fun GalleryOverviewView(
    pagesMap: Map<Int, StoryboardPage>,
    onSelectPage: (Int) -> Unit,
    onReturnToWorksheet: () -> Unit,
    onExportPdfClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("gallery_view"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "BOOK ONE: 25-PAGE PACING & PRODUCTION TRACKER",
                    fontFamily = CinzelFontFamily,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TerminalTextPrimary
                )
                Text(
                    text = "Select any of the 25 Storyboard Design Worksheets below to fill in its template fields.",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = TerminalTextSecondary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onExportPdfClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NostromoTeal,
                        contentColor = NostromoVoidBg
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("gallery_export_pdf_button")
                ) {
                    Text(
                        text = "Export Master PDF",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Button(
                    onClick = onReturnToWorksheet,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CommodoreAmber,
                        contentColor = NostromoVoidBg
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Return to Worksheet",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        HorizontalDivider(color = BulkheadDivider)

        for (p in 1..25) {
            val page = pagesMap[p] ?: StoryboardPage.createBlank(p)
            val phase = getPhaseForPage(p)
            val formattedPage = p.toString().padStart(2, '0')
            val sketchState = remember(page.sketchStrokes) {
                SketchCanvasState.deserialize(page.sketchStrokes)
            }

            Surface(
                color = NostromoSlateSurface,
                border = BorderStroke(1.dp, if (page.completed) MutterPhosphorGreen.copy(alpha = 0.6f) else BulkheadBorder),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectPage(p) }
                    .testTag("gallery_card_$p")
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // 16:9 Live Thumbnail Preview
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(NostromoVoidBg)
                            .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val scaleX = size.width / 800f
                            val scaleY = size.height / 450f
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
                            sketchState.layers.forEach { layer ->
                                if (layer.visible) {
                                    layer.stamps.forEach { stamp ->
                                        drawStampGuide(stamp, scaleX, scaleY)
                                    }
                                    layer.strokes.forEach { stroke ->
                                        drawSketchPoints(
                                            points = stroke.points,
                                            color = parseHexColor(stroke.colorHex),
                                            strokeWidth = stroke.strokeWidth * scaleX,
                                            scaleX = scaleX,
                                            scaleY = scaleY
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            color = NostromoVoidBg.copy(alpha = 0.88f),
                            border = BorderStroke(1.dp, CommodoreAmber.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Page $formattedPage of 25",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CommodoreAmberBright,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Surface(
                            color = NostromoVoidBg.copy(alpha = 0.90f),
                            border = BorderStroke(
                                1.dp,
                                if (page.completed) MutterPhosphorGreen.copy(alpha = 0.4f) else BulkheadBorder
                            ),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (page.completed) "☑ Completed" else "☐ Pending",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = if (page.completed) MutterPhosphorGreen else TerminalTextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Card Details
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "${phase.code.uppercase()} • ${phase.title}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NostromoTealBright
                        )
                        Text(
                            text = if (page.storyline.isNotBlank()) page.storyline else phase.focus,
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = if (page.storyline.isNotBlank()) TerminalTextPrimary else TerminalTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        HorizontalDivider(color = BulkheadDivider, modifier = Modifier.padding(vertical = 4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Illustrative Mandate: ${page.mandate.ifBlank { "Unknown / TBD" }}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = TerminalTextSecondary
                            )
                            Text(
                                text = "Open Sheet →",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CommodoreAmberBright
                            )
                        }
                    }
                }
            }
        }
    }
}
