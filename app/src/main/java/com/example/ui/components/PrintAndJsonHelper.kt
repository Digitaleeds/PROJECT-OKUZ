package com.example.ui.components

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.StoryboardPage
import com.example.data.model.getPhaseForPage
import com.example.ui.theme.BulkheadBorder
import com.example.ui.theme.CommodoreAmber
import com.example.ui.theme.CommodoreAmberBright
import com.example.ui.theme.FireflyBrassBorder
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

fun printStoryboardWorksheet(context: Context, page: StoryboardPage) {
    val phase = getPhaseForPage(page.pageNumber)
    val formattedPage = page.pageNumber.toString().padStart(2, '0')
    val rulesText = page.activeRuleLabels().joinToString(", ").ifBlank { "None Checked" }

    val html = """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8" />
          <style>
            body { font-family: sans-serif; color: #111827; padding: 24px; }
            .sheet { border: 2px solid #000; padding: 20px; border-radius: 8px; }
            .header { display: flex; justify-content: space-between; border-bottom: 2px solid #000; padding-bottom: 12px; margin-bottom: 12px; }
            .title { font-size: 20px; font-weight: bold; letter-spacing: 1px; }
            .meta { font-family: monospace; font-size: 12px; }
            .phase { background: #f3f4f6; padding: 8px 12px; border: 1px solid #d1d5db; margin-bottom: 16px; font-size: 12px; }
            .frame { border: 2px dashed #374151; height: 220px; display: flex; align-items: center; justify-content: center; flex-direction: column; margin-bottom: 16px; }
            .section { border: 1px solid #9ca3af; padding: 10px; border-radius: 6px; margin-bottom: 10px; }
            .sec-title { font-family: monospace; font-size: 11px; font-weight: bold; text-transform: uppercase; margin-bottom: 4px; }
            .field { font-size: 13px; margin: 4px 0; }
            .footer { border-top: 1px solid #9ca3af; padding-top: 8px; font-family: monospace; font-size: 10px; margin-top: 12px; }
          </style>
        </head>
        <body>
          <div class="sheet">
            <div class="header">
              <div>
                <div class="meta">THEME: EMOTIONAL INTELLIGENCE • BOOK ONE: SELF-REGULATION</div>
                <div class="title">PROJECT OKUZ — STORYBOARD DESIGN WORKSHEET</div>
              </div>
              <div class="meta">
                <strong>Page $formattedPage of 25</strong><br/>
                Status: ${if (page.completed) "COMPLETED" else "IN PROGRESS"}
              </div>
            </div>
            <div class="phase">
              <strong>${phase.code}: ${phase.title}</strong><br/>
              <em>${phase.focus}</em>
            </div>
            <div class="frame">
              <strong>THUMBNAIL SKETCH / CINEMATIC FRAMING (16:9 / SPREAD)</strong>
              <span>Camera Distance: ${page.cameraDistance.ifBlank { "Unselected" }} | Separation Force: ${page.separationForce.ifBlank { "Unselected" }}</span>
              <span>Main Character Rule Check: $rulesText | Mandate: ${page.mandate}</span>
            </div>
            <div class="section">
              <div class="sec-title">1. STORYBOARD STORYLINE (HAND-WRITTEN PAGE TEXT)</div>
              <div class="field">${page.storyline.ifBlank { "—" }}</div>
            </div>
            <div class="section">
              <div class="sec-title">2. ILLUSTRATION DESCRIPTION & FACE-OBSCURING ENGINEERING</div>
              <div class="field">${page.illustration.ifBlank { "—" }}</div>
            </div>
            <div class="section">
              <div class="sec-title">3. CHARACTER DESIGN NOTES</div>
              <div class="field"><strong>Kid Posture / Hood:</strong> ${page.kidPosture.ifBlank { "—" }}</div>
              <div class="field"><strong>Chiweenie Action / Ears:</strong> ${page.dogAction.ifBlank { "—" }}</div>
              <div class="field"><strong>Physical Distance Apart:</strong> ${page.distanceApart.ifBlank { "—" }}</div>
            </div>
            <div class="section">
              <div class="sec-title">4. STORY ARCS (SELF-REGULATION BEAT)</div>
              <div class="field"><strong>Emotional Trigger:</strong> ${page.trigger.ifBlank { "—" }}</div>
              <div class="field"><strong>Self-Regulation Response:</strong> ${page.response.ifBlank { "—" }}</div>
              <div class="field"><strong>Arc Turning Point:</strong> ${page.turningPoint.ifBlank { "—" }}</div>
            </div>
            <div class="section">
              <div class="sec-title">5. WORLD ENVIRONMENT ARC</div>
              <div class="field"><strong>Terrain / Weather Hazard:</strong> ${page.terrain.ifBlank { "—" }}</div>
              <div class="field"><strong>Environmental Obstacle Keeping Them Apart:</strong> ${page.obstacle.ifBlank { "—" }}</div>
            </div>
            <div class="footer">
              PROJECT OKUZ - Printable Storyboard Production Sheet (Duplicate 25x for Full Book) Worksheet Template. Page $formattedPage of 25
            </div>
          </div>
        </body>
        </html>
    """.trimIndent()

    val webView = WebView(context)
    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            val jobName = "Project_Okuz_Page_$formattedPage"
            val printAdapter = view.createPrintDocumentAdapter(jobName)
            printManager?.print(jobName, printAdapter, PrintAttributes.Builder().build())
        }
    }
    webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
}

@Composable
fun JsonImportExportDialog(
    initialJson: String,
    onDismiss: () -> Unit,
    onExportToFileClick: () -> Unit,
    onImportFromFileClick: () -> Unit,
    onExportPdfClick: () -> Unit = {},
    onExportPngClick: () -> Unit = {},
    onImportRawJson: (String) -> Unit
) {
    var jsonText by remember(initialJson) { mutableStateOf(initialJson) }
    var statusNotice by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = NostromoSlateSurface,
            shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp, topEnd = 4.dp, bottomStart = 4.dp),
            border = BorderStroke(1.5.dp, FireflyBrassBorder),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("json_dialog")
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "C64 // MU/TH/UR 6000 TELEMETRY & MASTER EXPORT",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CommodoreAmber
                )
                Text(
                    text = "Export all 25 Storyboard Worksheets as a multi-page cinematic PDF with layer breakdown or raw JSON buffer.",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = TerminalTextSecondary
                )

                // Master PDF Export Banner Button
                Button(
                    onClick = onExportPdfClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NostromoTeal,
                        contentColor = NostromoVoidBg
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_export_pdf_button")
                ) {
                    Text(
                        text = "EXPORT 25-PAGE MASTER PDF (PRESERVING LAYERS)",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Save Current Canvas PNG Button
                Button(
                    onClick = {
                        onExportPngClick()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NostromoPanelCard,
                        contentColor = TerminalTextPrimary
                    ),
                    shape = RoundedCornerShape(6.dp),
                    border = BorderStroke(1.dp, NostromoTeal.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_export_png_button")
                ) {
                    Text(
                        text = "SAVE CURRENT CANVAS AS PNG IMAGE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NostromoTealBright
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportToFileClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CommodoreAmber,
                            contentColor = NostromoVoidBg
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Save .JSON File",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = onImportFromFileClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NostromoPanelCard,
                            contentColor = TerminalTextPrimary
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Load .JSON File",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    textStyle = TextStyle(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = MutterPhosphorGreen
                    ),
                    minLines = 7,
                    maxLines = 10,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CommodoreAmber,
                        unfocusedBorderColor = BulkheadBorder,
                        focusedContainerColor = NostromoRecessedWell,
                        unfocusedContainerColor = NostromoRecessedWell
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .testTag("json_buffer_input")
                )

                if (statusNotice != null) {
                    Text(
                        text = statusNotice!!,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = CommodoreAmberBright
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(jsonText))
                            statusNotice = "COPIED 25-PAGE JSON TO CLIPBOARD"
                        }
                    ) {
                        Text(
                            text = "Copy JSON",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = CommodoreAmberBright
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                text = "Close",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = TerminalTextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { onImportRawJson(jsonText) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MutterPhosphorGreen,
                                contentColor = NostromoVoidBg
                            ),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Apply JSON",
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
}
