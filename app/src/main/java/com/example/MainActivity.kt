package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.model.EQ_FIVE_YEAR_PILLARS
import com.example.data.model.StoryboardPage
import com.example.data.repository.StoryboardRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOutUser
import com.example.ui.components.GalleryOverviewView
import com.example.ui.components.JsonImportExportDialog
import com.example.ui.components.MasterBlueprintView
import com.example.ui.components.PhaseTrackerPanel
import com.example.ui.components.PngExportHelper
import com.example.ui.components.WorksheetView
import com.example.ui.components.printStoryboardWorksheet
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
import com.example.ui.theme.NostromoVoidBg
import com.example.ui.theme.ProjectOkuzTheme
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.example.ui.viewmodel.StoryboardViewModel
import com.example.ui.viewmodel.StudioTab
import com.example.ui.viewmodel.SyncStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ui.components.PdfExportHelper

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProjectOkuzTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = FirebaseAuth.getInstance()) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        StudioMainScreen(currentUserId = user.uid)
    }
}

@Composable
fun StudioMainScreen(
    currentUserId: String,
    viewModel: StoryboardViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                StoryboardViewModel(StoryboardRepository(db), currentUserId)
            }
        }
    )
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val pagesMap by viewModel.pagesMap.collectAsStateWithLifecycle()
    val currentPageNumber by viewModel.currentPageNumber.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()
    val sketchState by viewModel.sketchState.collectAsStateWithLifecycle()
    val drawTool by viewModel.drawTool.collectAsStateWithLifecycle()
    val penColorHex by viewModel.penColorHex.collectAsStateWithLifecycle()

    var showJsonDialog by remember { mutableStateOf(false) }

    val currentPage = pagesMap[currentPageNumber]
        ?: StoryboardPage.createBlank(currentPageNumber, currentUserId)

    // BackHandler for secondary tabs
    BackHandler(enabled = activeTab != StudioTab.WORKSHEET) {
        viewModel.switchTab(StudioTab.WORKSHEET)
    }

    // Document Export Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            try {
                val jsonBytes = viewModel.exportProjectJson().toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(jsonBytes)
                }
                Toast.makeText(context, "Saved project_okuz_book1_storyboards.json", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Document Import Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!text.isNullOrBlank()) {
                    viewModel.importProjectJson(text) { _, message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                    showJsonDialog = false
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Document Master PDF Export Launcher
    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        PdfExportHelper.generateMasterPdf(context, pagesMap, out)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Exported 25-Page Master Storyboard PDF (Layers Preserved)!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "PDF export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // Canvas PNG Image Export Launcher (SAF Document Provider)
    val pngExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                try {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        PngExportHelper.exportCanvasToStream(
                            sketchState = sketchState,
                            pageNumber = currentPage.pageNumber,
                            cameraDistance = currentPage.cameraDistance,
                            separationForce = currentPage.separationForce,
                            outputStream = out
                        )
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Saved Page ${currentPage.pageNumber.toString().padStart(2, '0')} Canvas as PNG Image!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "PNG export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    val onExportPngClick: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            // First attempt to save directly to device Pictures / ProjectOkuz
            val result = PngExportHelper.saveToDevicePictures(
                context = context,
                sketchState = sketchState,
                pageNumber = currentPage.pageNumber,
                cameraDistance = currentPage.cameraDistance,
                separationForce = currentPage.separationForce
            )
            if (result.isSuccess) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        context,
                        "Saved Page ${currentPage.pageNumber.toString().padStart(2, '0')} Canvas to Device Pictures/ProjectOkuz!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                // Fallback: try app local files directory, or open SAF launcher
                val fallbackFile = PngExportHelper.saveToLocalFile(
                    context = context,
                    sketchState = sketchState,
                    pageNumber = currentPage.pageNumber,
                    cameraDistance = currentPage.cameraDistance,
                    separationForce = currentPage.separationForce
                )
                withContext(Dispatchers.Main) {
                    if (fallbackFile.isSuccess) {
                        Toast.makeText(
                            context,
                            "Saved Page ${currentPage.pageNumber.toString().padStart(2, '0')} Canvas to local storage (${fallbackFile.getOrNull()?.name})",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        val formatted = currentPage.pageNumber.toString().padStart(2, '0')
                        pngExportLauncher.launch("project_okuz_p${formatted}_sketch.png")
                    }
                }
            }
        }
    }

    if (showJsonDialog) {
        JsonImportExportDialog(
            initialJson = viewModel.exportProjectJson(),
            onDismiss = { showJsonDialog = false },
            onExportToFileClick = {
                exportLauncher.launch("project_okuz_book1_storyboards.json")
            },
            onImportFromFileClick = {
                importLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
            },
            onExportPdfClick = {
                pdfExportLauncher.launch("project_okuz_book1_master_storyboard.pdf")
            },
            onExportPngClick = onExportPngClick,
            onImportRawJson = { raw ->
                viewModel.importProjectJson(raw) { ok, msg ->
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    if (ok) showJsonDialog = false
                }
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = NostromoVoidBg,
        topBar = {
            StudioTopHeader(
                syncStatus = syncStatus,
                completedCount = pagesMap.values.count { it.completed },
                onOpenJsonDialog = { showJsonDialog = true },
                onPrintSheet = { printStoryboardWorksheet(context, currentPage) },
                onExportPdfClick = {
                    pdfExportLauncher.launch("project_okuz_book1_master_storyboard.pdf")
                },
                onSignOut = {
                    signOutUser(
                        credentialManager = credentialManager,
                        onSignOutComplete = {},
                        scope = scope
                    )
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = NostromoSlateSurface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(BorderStroke(1.dp, BulkheadDivider))
                    .navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeTab == StudioTab.WORKSHEET,
                    onClick = { viewModel.switchTab(StudioTab.WORKSHEET) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Storyboard Worksheet"
                        )
                    },
                    label = {
                        Text(
                            text = "Worksheet",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp
                        )
                    },
                    colors = nostromoNavColors(),
                    modifier = Modifier.testTag("tab_worksheet_btn")
                )
                NavigationBarItem(
                    selected = activeTab == StudioTab.TRACKER,
                    onClick = { viewModel.switchTab(StudioTab.TRACKER) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Checklist,
                            contentDescription = "25-Page Pacing Tracker"
                        )
                    },
                    label = {
                        Text(
                            text = "Tracker",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp
                        )
                    },
                    colors = nostromoNavColors(),
                    modifier = Modifier.testTag("tab_tracker_btn")
                )
                NavigationBarItem(
                    selected = activeTab == StudioTab.BLUEPRINT,
                    onClick = { viewModel.switchTab(StudioTab.BLUEPRINT) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = "Master Series Blueprint"
                        )
                    },
                    label = {
                        Text(
                            text = "Blueprint",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp
                        )
                    },
                    colors = nostromoNavColors(),
                    modifier = Modifier.testTag("tab_blueprint_btn")
                )
                NavigationBarItem(
                    selected = activeTab == StudioTab.GALLERY,
                    onClick = { viewModel.switchTab(StudioTab.GALLERY) },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "25-Page Overview"
                        )
                    },
                    label = {
                        Text(
                            text = "25-Pages",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp
                        )
                    },
                    colors = nostromoNavColors(),
                    modifier = Modifier.testTag("tab_gallery_btn")
                )
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isExpandedScreen = maxWidth >= 840.dp

            if (isExpandedScreen) {
                // Expanded / Tablet Layout: Left Sidebar 25-Page Pacing Tracker + Right Main Workspace
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .width(320.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        PhaseTrackerPanel(
                            pagesMap = pagesMap,
                            currentPageNumber = currentPageNumber,
                            onSelectPage = { p -> viewModel.selectPage(p, switchToWorksheet = true) }
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ErrorBannerIfAny(errorBanner, onDismiss = viewModel::dismissErrorBanner)
                        ActiveWorkspaceContent(
                            activeTab = activeTab,
                            currentPage = currentPage,
                            pagesMap = pagesMap,
                            currentPageNumber = currentPageNumber,
                            sketchState = sketchState,
                            drawTool = drawTool,
                            penColorHex = penColorHex,
                            viewModel = viewModel,
                            onExportPdfClick = {
                                pdfExportLauncher.launch("project_okuz_book1_master_storyboard.pdf")
                            },
                            onExportPngClick = onExportPngClick
                        )
                    }
                }
            } else {
                // Compact Mobile Layout: Quick 1..25 Page Strip + Scrollable Active View
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Compact Horizontal Page Jump Ribbon (Pages 01..25)
                    QuickPageJumpRibbon(
                        pagesMap = pagesMap,
                        currentPageNumber = currentPageNumber,
                        onSelectPage = { p -> viewModel.selectPage(p, switchToWorksheet = true) }
                    )

                    ErrorBannerIfAny(errorBanner, onDismiss = viewModel::dismissErrorBanner)

                    ActiveWorkspaceContent(
                        activeTab = activeTab,
                        currentPage = currentPage,
                        pagesMap = pagesMap,
                        currentPageNumber = currentPageNumber,
                        sketchState = sketchState,
                        drawTool = drawTool,
                        penColorHex = penColorHex,
                        viewModel = viewModel,
                        onExportPdfClick = {
                            pdfExportLauncher.launch("project_okuz_book1_master_storyboard.pdf")
                        },
                        onExportPngClick = onExportPngClick
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveWorkspaceContent(
    activeTab: StudioTab,
    currentPage: StoryboardPage,
    pagesMap: Map<Int, StoryboardPage>,
    currentPageNumber: Int,
    sketchState: com.example.data.model.SketchCanvasState,
    drawTool: com.example.ui.viewmodel.DrawTool,
    penColorHex: String,
    viewModel: StoryboardViewModel,
    onExportPdfClick: () -> Unit = {},
    onExportPngClick: () -> Unit = {}
) {
    when (activeTab) {
        StudioTab.WORKSHEET -> {
            val canUndo by viewModel.canUndoSketch.collectAsStateWithLifecycle()
            val canRedo by viewModel.canRedoSketch.collectAsStateWithLifecycle()
            val undoCount by viewModel.undoHistoryCount.collectAsStateWithLifecycle()
            val redoCount by viewModel.redoHistoryCount.collectAsStateWithLifecycle()

            WorksheetView(
                page = currentPage,
                sketchState = sketchState,
                drawTool = drawTool,
                penColorHex = penColorHex,
                onToggleComplete = viewModel::togglePageComplete,
                onUpdateRuleCheck = viewModel::updateRuleCheck,
                onSetCameraDistance = viewModel::setCameraDistance,
                onSetSeparationForce = viewModel::setSeparationForce,
                onUpdateField = viewModel::updateField,
                onSetDrawTool = viewModel::setDrawTool,
                onSetPenColor = viewModel::setPenColor,
                onAddStroke = viewModel::addStrokeToActiveLayer,
                onStampMarker = viewModel::stampMarkerOnActiveLayer,
                onUndo = viewModel::undoOnActiveLayer,
                onRedo = viewModel::redoOnActiveLayer,
                canUndo = canUndo,
                canRedo = canRedo,
                undoCount = undoCount,
                redoCount = redoCount,
                onClearActiveLayer = viewModel::clearActiveLayer,
                onSelectLayer = viewModel::selectLayer,
                onAddLayer = { viewModel.addLayer() },
                onDeleteLayer = viewModel::deleteLayer,
                onMoveLayerUp = viewModel::moveLayerUp,
                onMoveLayerDown = viewModel::moveLayerDown,
                onToggleLayerVisibility = viewModel::toggleLayerVisibility,
                onRenameLayer = viewModel::renameLayer,
                onSetLayerOpacity = viewModel::setLayerOpacity,
                onMergeLayerDown = viewModel::mergeLayerDown,
                onFlattenAllLayers = viewModel::flattenAllLayers,
                onToggleSpreadMode = { viewModel.toggleTwoPageSpread() },
                onCombineTwoPagesAsOne = { facing, mergeText ->
                    viewModel.combineTwoPagesAsOneFull(facing, mergeText)
                },
                onExportPng = onExportPngClick,
                onResetEntirePage = viewModel::resetCurrentPage,
                onPrevPage = viewModel::prevPage,
                onNextPage = viewModel::nextPage
            )
        }
        StudioTab.TRACKER -> {
            PhaseTrackerPanel(
                pagesMap = pagesMap,
                currentPageNumber = currentPageNumber,
                onSelectPage = { p -> viewModel.selectPage(p, switchToWorksheet = true) }
            )
        }
        StudioTab.BLUEPRINT -> {
            MasterBlueprintView(
                pagesMap = pagesMap,
                onSelectPage = { p -> viewModel.selectPage(p, switchToWorksheet = true) }
            )
        }
        StudioTab.GALLERY -> {
            GalleryOverviewView(
                pagesMap = pagesMap,
                onSelectPage = { p -> viewModel.selectPage(p, switchToWorksheet = true) },
                onReturnToWorksheet = { viewModel.switchTab(StudioTab.WORKSHEET) },
                onExportPdfClick = onExportPdfClick
            )
        }
    }
}

@Composable
private fun StudioTopHeader(
    syncStatus: SyncStatus,
    completedCount: Int,
    onOpenJsonDialog: () -> Unit,
    onPrintSheet: () -> Unit,
    onExportPdfClick: () -> Unit = {},
    onSignOut: () -> Unit
) {
    Surface(
        color = NostromoSlateSurface,
        border = BorderStroke(1.dp, BulkheadDivider),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand Identity
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = CommodoreAmber,
                        shape = CutCornerShape(6.dp),
                        border = BorderStroke(1.dp, FireflyCopper),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "O",
                                fontFamily = CinzelFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = NostromoVoidBg
                            )
                        }
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "PROJECT OKUZ",
                                fontFamily = CinzelFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = TerminalTextPrimary
                            )
                            Surface(
                                color = CommodoreAmber.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, CommodoreAmber.copy(alpha = 0.35f)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "YR 1 • SELF-REGULATION",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CommodoreAmberBright,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "25-Page Handwritten Storyboard & Cinematic Studio",
                            fontFamily = InterFontFamily,
                            fontSize = 11.sp,
                            color = TerminalTextSecondary
                        )
                    }
                }

                // Action Buttons: Auto-Saved Status, JSON Export/Import, Print Sheet, Sign Out
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val statusColor = when (syncStatus) {
                        SyncStatus.AUTO_SAVED, SyncStatus.SAVED -> MutterPhosphorGreen
                        SyncStatus.SAVING -> CommodoreAmberBright
                        SyncStatus.ERROR -> HazardRose
                    }
                    Surface(
                        color = statusColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f)),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.testTag("save_status_badge")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(statusColor, CircleShape)
                            )
                            Text(
                                text = syncStatus.badgeText,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenJsonDialog,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("export_import_json_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Export / Import JSON",
                            tint = CommodoreAmberBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onExportPdfClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("export_master_pdf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export 25-Page Master PDF (Preserving Layers)",
                            tint = CommodoreAmberBright,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onPrintSheet,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("print_sheet_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print Sheet",
                            tint = NostromoTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = TerminalTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // EQ 5-YEAR PILLAR BAR (Scrollable on compact screens)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NostromoRecessedWell, RoundedCornerShape(6.dp))
                    .border(1.dp, BulkheadBorder, RoundedCornerShape(6.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EQ_FIVE_YEAR_PILLARS.forEach { pillar ->
                    Surface(
                        color = if (pillar.isActiveBook) CommodoreAmber else NostromoVoidBg,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (pillar.isActiveBook) "YR 1  ${pillar.pillarTitle}" else pillar.pillarTitle,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = if (pillar.isActiveBook) FontWeight.Bold else FontWeight.Normal,
                            color = if (pillar.isActiveBook) NostromoVoidBg else TerminalTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DONE: $completedCount/25",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = CommodoreAmberBright
                )
            }
        }
    }
}

@Composable
private fun QuickPageJumpRibbon(
    pagesMap: Map<Int, StoryboardPage>,
    currentPageNumber: Int,
    onSelectPage: (Int) -> Unit
) {
    Surface(
        color = NostromoSlateSurface,
        border = BorderStroke(1.dp, BulkheadBorder),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PAGES:",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = CommodoreAmber,
                modifier = Modifier.padding(end = 4.dp)
            )
            for (p in 1..25) {
                val isCurrent = p == currentPageNumber
                val isDone = pagesMap[p]?.completed == true
                Surface(
                    color = when {
                        isCurrent -> CommodoreAmber
                        isDone -> MutterPhosphorGreen.copy(alpha = 0.18f)
                        else -> NostromoRecessedWell
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isCurrent -> CommodoreAmberBright
                            isDone -> MutterPhosphorGreen
                            else -> BulkheadBorder
                        }
                    ),
                    shape = RoundedCornerShape(5.dp),
                    modifier = Modifier
                        .clickable { onSelectPage(p) }
                        .testTag("ribbon_page_$p")
                ) {
                    Text(
                        text = "${p.toString().padStart(2, '0')}${if (isDone) "✓" else ""}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = if (isCurrent || isDone) FontWeight.Bold else FontWeight.Normal,
                        color = when {
                            isCurrent -> NostromoVoidBg
                            isDone -> MutterPhosphorGreen
                            else -> TerminalTextPrimary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ErrorBannerIfAny(
    errorBanner: String?,
    onDismiss: () -> Unit
) {
    if (errorBanner != null) {
        Surface(
            color = HazardRose.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, HazardRose),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onDismiss() }
        ) {
            Text(
                text = "TELEMETRY NOTICE: $errorBanner (Tap to dismiss)",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                color = HazardRose,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}

@Composable
private fun nostromoNavColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = NostromoVoidBg,
    selectedTextColor = CommodoreAmberBright,
    indicatorColor = CommodoreAmber,
    unselectedIconColor = TerminalTextSecondary,
    unselectedTextColor = TerminalTextSecondary
)
