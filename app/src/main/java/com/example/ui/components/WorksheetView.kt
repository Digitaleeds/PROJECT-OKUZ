package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CAMERA_DISTANCE_OPTIONS
import com.example.data.model.SEPARATION_FORCE_OPTIONS
import com.example.data.model.SketchCanvasState
import com.example.data.model.SketchPoint
import com.example.data.model.StampType
import com.example.data.model.StoryboardPage
import com.example.data.model.getPhaseForPage
import com.example.ui.theme.BulkheadBorder
import com.example.ui.theme.BulkheadDivider
import com.example.ui.theme.CaveatFontFamily
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
import com.example.ui.viewmodel.DrawTool
import com.example.ui.viewmodel.PageField

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorksheetView(
    page: StoryboardPage,
    sketchState: SketchCanvasState,
    drawTool: DrawTool,
    penColorHex: String,
    onToggleComplete: () -> Unit,
    onUpdateRuleCheck: (String, Boolean) -> Unit,
    onSetCameraDistance: (String) -> Unit,
    onSetSeparationForce: (String) -> Unit,
    onUpdateField: (PageField, String) -> Unit,
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
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    val phase = getPhaseForPage(page.pageNumber)
    val formattedPage = page.pageNumber.toString().padStart(2, '0')

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = NostromoSlateSurface,
        shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp),
        border = BorderStroke(1.5.dp, FireflyBrassBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // WORKSHEET HEADER BANNER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NostromoVoidBg)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "THEME: EMOTIONAL INTELLIGENCE • BOOK ONE: SELF-REGULATION",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = CommodoreAmber
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "STORYBOARD DESIGN WORKSHEET",
                            fontFamily = CinzelFontFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerminalTextPrimary
                        )
                    }

                    // Page Number Display Badge
                    Surface(
                        color = NostromoPanelCard,
                        border = BorderStroke(1.dp, FireflyBrassBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page ",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerminalTextPrimary
                            )
                            Text(
                                text = formattedPage,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = CommodoreAmberBright,
                                modifier = Modifier.testTag("current_page_display")
                            )
                            Text(
                                text = " of 25",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TerminalTextPrimary
                            )
                        }
                    }
                }

                // Quick Prev / Next + Page Check-Off Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = NostromoPanelCard,
                            border = BorderStroke(1.dp, BulkheadBorder),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clickable(enabled = page.pageNumber > 1) { onPrevPage() }
                                .testTag("prev_page_top_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Previous Page",
                                    tint = if (page.pageNumber > 1) CommodoreAmberBright else TerminalTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "PREV",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (page.pageNumber > 1) TerminalTextPrimary else TerminalTextSecondary
                                )
                            }
                        }

                        Surface(
                            color = NostromoPanelCard,
                            border = BorderStroke(1.dp, BulkheadBorder),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .clickable(enabled = page.pageNumber < 25) { onNextPage() }
                                .testTag("next_page_top_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "NEXT",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (page.pageNumber < 25) TerminalTextPrimary else TerminalTextSecondary
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Next Page",
                                    tint = if (page.pageNumber < 25) CommodoreAmberBright else TerminalTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }

                    // PAGE CHECK-OFF Toggle
                    Surface(
                        color = if (page.completed) MutterPhosphorGreen.copy(alpha = 0.16f) else NostromoPanelCard,
                        border = BorderStroke(
                            1.dp,
                            if (page.completed) MutterPhosphorGreen else BulkheadBorder
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .clickable { onToggleComplete() }
                            .testTag("page_completed_checkbox")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (page.completed) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "Page Check-Off",
                                tint = if (page.completed) MutterPhosphorGreen else CommodoreAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (page.completed) "PAGE CHECK-OFF: DONE" else "PAGE CHECK-OFF",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (page.completed) MutterPhosphorGreen else TerminalTextPrimary
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = BulkheadDivider)

            // PHASE CONTEXT BAR
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NostromoPanelCard.copy(alpha = 0.85f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = NostromoTeal.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, NostromoTeal.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = phase.code.uppercase(),
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NostromoTealBright,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = phase.title,
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalTextPrimary
                    )
                }
                Text(
                    text = phase.focus,
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    color = TerminalTextSecondary
                )
            }

            HorizontalDivider(color = BulkheadDivider)

            // WORKSHEET BODY SECTIONS
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 16:9 MULTI-LAYER SKETCHPAD
                MultiLayerSketchPad(
                    pageNumber = page.pageNumber,
                    cameraDistance = page.cameraDistance,
                    activeRuleLabels = page.activeRuleLabels(),
                    sketchState = sketchState,
                    drawTool = drawTool,
                    penColorHex = penColorHex,
                    onSetDrawTool = onSetDrawTool,
                    onSetPenColor = onSetPenColor,
                    onAddStroke = onAddStroke,
                    onStampMarker = onStampMarker,
                    onUndo = onUndo,
                    onRedo = onRedo,
                    canUndo = canUndo,
                    canRedo = canRedo,
                    undoCount = undoCount,
                    redoCount = redoCount,
                    onClearActiveLayer = onClearActiveLayer,
                    onSelectLayer = onSelectLayer,
                    onAddLayer = onAddLayer,
                    onDeleteLayer = onDeleteLayer,
                    onMoveLayerUp = onMoveLayerUp,
                    onMoveLayerDown = onMoveLayerDown,
                    onToggleLayerVisibility = onToggleLayerVisibility,
                    onRenameLayer = onRenameLayer,
                    onSetLayerOpacity = onSetLayerOpacity,
                    onMergeLayerDown = onMergeLayerDown,
                    onFlattenAllLayers = onFlattenAllLayers,
                    onToggleSpreadMode = onToggleSpreadMode,
                    onCombineTwoPagesAsOne = onCombineTwoPagesAsOne,
                    onExportPng = onExportPng,
                    onResetEntirePage = onResetEntirePage
                )

                // MAIN CHARACTER RULE CHECK & ILLUSTRATIVE MANDATE PANEL
                MainCharacterRuleCheckPanel(
                    page = page,
                    onUpdateRuleCheck = onUpdateRuleCheck,
                    onUpdateMandate = { onUpdateField(PageField.MANDATE, it) }
                )

                // SECTION 1: 1. STORYBOARD STORYLINE (HAND-WRITTEN PAGE TEXT)
                WorksheetSectionCard(
                    title = "1. STORYBOARD STORYLINE (HAND-WRITTEN PAGE TEXT)",
                    subtitle = "Exact manuscript lines & pacing beats to be hand-written on this page",
                    accentColor = CommodoreAmber
                ) {
                    OutlinedTextField(
                        value = page.storyline,
                        onValueChange = { onUpdateField(PageField.STORYLINE, it) },
                        placeholder = {
                            Text(
                                text = "Exact manuscript lines & pacing beats to be hand-written on this page...",
                                fontFamily = CaveatFontFamily,
                                fontSize = 20.sp,
                                color = TerminalTextSecondary.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = CaveatFontFamily,
                            fontSize = 22.sp,
                            lineHeight = 28.sp,
                            color = Color(0xFFFEF3C7)
                        ),
                        minLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CommodoreAmber,
                            unfocusedBorderColor = CommodoreAmber.copy(alpha = 0.35f),
                            focusedContainerColor = CommodoreAmber.copy(alpha = 0.05f),
                            unfocusedContainerColor = CommodoreAmber.copy(alpha = 0.03f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_storyline")
                    )
                }

                // SECTION 2: 2. ILLUSTRATION DESCRIPTION & FACE-OBSCURING ENGINEERING
                WorksheetSectionCard(
                    title = "2. ILLUSTRATION DESCRIPTION & FACE-OBSCURING ENGINEERING",
                    subtitle = "Dramatic-cinematic staging, lighting, camera angle & exact method hiding the child's face",
                    accentColor = CommodoreAmber
                ) {
                    OutlinedTextField(
                        value = page.illustration,
                        onValueChange = { onUpdateField(PageField.ILLUSTRATION, it) },
                        placeholder = {
                            Text(
                                text = "Dramatic-cinematic staging, lighting, camera angle & exact method hiding the child's face",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = TerminalTextSecondary.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = TextStyle(
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = TerminalTextPrimary
                        ),
                        minLines = 3,
                        colors = NostromoTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("field_illustration")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = BulkheadDivider)
                    Spacer(modifier = Modifier.height(10.dp))

                    // CAMERA DISTANCE & SEPARATION FORCE
                    Text(
                        text = "CAMERA DISTANCE:",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CAMERA_DISTANCE_OPTIONS.forEach { cam ->
                            val selected = page.cameraDistance == cam
                            OptionChipButton(
                                text = cam,
                                selected = selected,
                                onClick = { onSetCameraDistance(cam) },
                                tag = "cam_${cam.lowercase().replace(" ", "_")}"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "SEPARATION FORCE:",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TerminalTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SEPARATION_FORCE_OPTIONS.forEach { force ->
                            val selected = page.separationForce == force
                            OptionChipButton(
                                text = force,
                                selected = selected,
                                onClick = { onSetSeparationForce(force) },
                                tag = "sep_${force.lowercase().replace(" ", "_")}"
                            )
                        }
                    }
                }

                // SECTION 3: 3. CHARACTER DESIGN NOTES
                WorksheetSectionCard(
                    title = "3. CHARACTER DESIGN NOTES",
                    subtitle = "Hooded 10-Yr-Old posture/gear & pointy-eared Chiweenie body language",
                    accentColor = CommodoreAmber
                ) {
                    LabeledInputRow(
                        label = "Kid Posture / Hood:",
                        value = page.kidPosture,
                        onValueChange = { onUpdateField(PageField.KID_POSTURE, it) },
                        tag = "field_kidPosture"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LabeledInputRow(
                        label = "Chiweenie Action / Ears:",
                        value = page.dogAction,
                        onValueChange = { onUpdateField(PageField.DOG_ACTION, it) },
                        tag = "field_dogAction"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LabeledInputRow(
                        label = "Physical Distance Apart:",
                        value = page.distanceApart,
                        onValueChange = { onUpdateField(PageField.DISTANCE_APART, it) },
                        tag = "field_distanceApart",
                        useMonoAccent = true
                    )
                }

                // SECTION 4: 4. STORY ARCS (SELF-REGULATION BEAT)
                WorksheetSectionCard(
                    title = "4. STORY ARCS (SELF-REGULATION BEAT)",
                    subtitle = "Emotional trigger, impulse control, & fight to reunite",
                    accentColor = NostromoTeal
                ) {
                    LabeledInputRow(
                        label = "Emotional Trigger:",
                        value = page.trigger,
                        onValueChange = { onUpdateField(PageField.TRIGGER, it) },
                        tag = "field_trigger"
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LabeledInputRow(
                        label = "Self-Regulation Response:",
                        value = page.response,
                        onValueChange = { onUpdateField(PageField.RESPONSE, it) },
                        tag = "field_response",
                        useTealAccent = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LabeledInputRow(
                        label = "Arc Turning Point:",
                        value = page.turningPoint,
                        onValueChange = { onUpdateField(PageField.TURNING_POINT, it) },
                        tag = "field_turningPoint"
                    )
                }

                // SECTION 5: 5. WORLD ENVIRONMENT ARC
                WorksheetSectionCard(
                    title = "5. WORLD ENVIRONMENT ARC",
                    subtitle = "Harsh terrain, weather escalation, natural barriers, & atmospheric mystery",
                    accentColor = CommodoreAmber
                ) {
                    LabeledInputRow(
                        label = "Terrain / Weather Hazard:",
                        value = page.terrain,
                        onValueChange = { onUpdateField(PageField.TERRAIN, it) },
                        tag = "field_terrain",
                        minLines = 2
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LabeledInputRow(
                        label = "Environmental Obstacle Keeping Them Apart:",
                        value = page.obstacle,
                        onValueChange = { onUpdateField(PageField.OBSTACLE, it) },
                        tag = "field_obstacle",
                        minLines = 2
                    )
                }
            }

            HorizontalDivider(color = BulkheadDivider)

            // WORKSHEET FOOTER
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NostromoVoidBg)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "PROJECT OKUZ - Printable Storyboard Production Sheet (Duplicate 25x for Full Book) Worksheet Template. Page $formattedPage of 25",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = TerminalTextSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onPrevPage,
                        enabled = page.pageNumber > 1,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NostromoPanelCard,
                            contentColor = TerminalTextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("prev_page_footer_button")
                    ) {
                        Text(
                            text = "← Prev Page",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = onNextPage,
                        enabled = page.pageNumber < 25,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CommodoreAmber,
                            contentColor = NostromoVoidBg
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("next_page_footer_button")
                    ) {
                        Text(
                            text = "Next Page →",
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

@Composable
private fun MainCharacterRuleCheckPanel(
    page: StoryboardPage,
    onUpdateRuleCheck: (String, Boolean) -> Unit,
    onUpdateMandate: (String) -> Unit
) {
    Surface(
        color = NostromoRecessedWell,
        border = BorderStroke(1.dp, HazardRose.copy(alpha = 0.45f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Main Character Rule Check",
                    tint = HazardRose,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "MAIN CHARACTER RULE CHECK (PERMANENT FACE OBSCURATION)",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = HazardRose
                )
            }
            Text(
                text = "Check all face-obscuring staging methods used on Page ${page.pageNumber.toString().padStart(2, '0')}:",
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = TerminalTextSecondary
            )

            RuleCheckboxItem(
                label = "Viewed from behind / dorsal angle",
                checked = page.ruleDorsal,
                onCheckedChange = { onUpdateRuleCheck("dorsal", it) },
                tag = "rule_dorsal"
            )
            RuleCheckboxItem(
                label = "Deep hooded jacket shadow",
                checked = page.ruleHood,
                onCheckedChange = { onUpdateRuleCheck("hood", it) },
                tag = "rule_hood"
            )
            RuleCheckboxItem(
                label = "Eclipsed by tree / terrain",
                checked = page.ruleTree,
                onCheckedChange = { onUpdateRuleCheck("tree", it) },
                tag = "rule_tree"
            )
            RuleCheckboxItem(
                label = "Blocked by misc. foreground object",
                checked = page.ruleObject,
                onCheckedChange = { onUpdateRuleCheck("object", it) },
                tag = "rule_object"
            )

            Spacer(modifier = Modifier.height(4.dp))
            LabeledInputRow(
                label = "Illustrative Mandate:",
                value = page.mandate,
                onValueChange = onUpdateMandate,
                tag = "field_mandate",
                useMonoAccent = true
            )
        }
    }
}

@Composable
private fun RuleCheckboxItem(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    tag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = CommodoreAmber,
                checkmarkColor = NostromoVoidBg,
                uncheckedColor = TerminalTextSecondary
            ),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            color = if (checked) CommodoreAmberBright else TerminalTextPrimary
        )
    }
}

@Composable
private fun WorksheetSectionCard(
    title: String,
    subtitle: String,
    accentColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        color = NostromoRecessedWell,
        border = BorderStroke(1.dp, BulkheadBorder),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Text(
                text = title,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontFamily = InterFontFamily,
                fontSize = 11.sp,
                color = TerminalTextSecondary
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun OptionChipButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    tag: String
) {
    Surface(
        color = if (selected) CommodoreAmber else NostromoPanelCard,
        border = BorderStroke(
            1.dp,
            if (selected) CommodoreAmberBright else BulkheadBorder
        ),
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .clickable { onClick() }
            .testTag(tag)
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) NostromoVoidBg else TerminalTextPrimary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun LabeledInputRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    tag: String,
    minLines: Int = 1,
    useMonoAccent: Boolean = false,
    useTealAccent: Boolean = false
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = InterFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TerminalTextSecondary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = if (useMonoAccent) JetBrainsMonoFontFamily else InterFontFamily,
                fontSize = 12.sp,
                color = when {
                    useMonoAccent -> CommodoreAmberBright
                    useTealAccent -> NostromoTealBright
                    else -> TerminalTextPrimary
                }
            ),
            minLines = minLines,
            colors = NostromoTextFieldColors(useTealAccent = useTealAccent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(tag)
        )
    }
}

@Composable
private fun NostromoTextFieldColors(useTealAccent: Boolean = false) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = if (useTealAccent) NostromoTeal else CommodoreAmber,
        unfocusedBorderColor = BulkheadBorder,
        focusedContainerColor = NostromoSlateSurface,
        unfocusedContainerColor = NostromoSlateSurface
    )
