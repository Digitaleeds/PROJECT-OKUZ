package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.EQ_FIVE_YEAR_PILLARS
import com.example.ui.theme.BulkheadBorder
import com.example.ui.theme.CinzelFontFamily
import com.example.ui.theme.CommodoreAmber
import com.example.ui.theme.CommodoreAmberBright
import com.example.ui.theme.FireflyBrassBorder
import com.example.ui.theme.FireflyCopper
import com.example.ui.theme.HazardRose
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MutterPhosphorGreen
import com.example.ui.theme.NostromoPanelCard
import com.example.ui.theme.NostromoRecessedWell
import com.example.ui.theme.NostromoSlateSurface
import com.example.ui.theme.NostromoTeal
import com.example.ui.theme.NostromoVoidBg
import com.example.ui.theme.TerminalTextPrimary
import com.example.ui.theme.TerminalTextSecondary
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val auth = FirebaseAuth.getInstance()
    if (auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                auth.signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                FirebaseAuth.getInstance().signInWithCredential(authCredential).await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

fun signOutUser(
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    FirebaseAuth.getInstance().signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = coroutineScope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NostromoVoidBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            color = NostromoSlateSurface,
            shape = CutCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 4.dp, bottomStart = 4.dp),
            border = BorderStroke(1.5.dp, FireflyBrassBorder)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // MU/TH/UR 6000 + Commodore 64 Terminal Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NostromoRecessedWell, RoundedCornerShape(6.dp))
                        .border(1.dp, BulkheadBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "Terminal Console",
                            tint = CommodoreAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MU/TH/UR 6000 // C64 BRIDGE CONSOLE",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = CommodoreAmber
                        )
                    }
                    Text(
                        text = "SYS.RDY █",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = MutterPhosphorGreen
                    )
                }

                // CRT Vector Emblem + Title Block
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    NostromoRadarEmblem(modifier = Modifier.size(84.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "PROJECT OKUZ",
                            fontFamily = CinzelFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = TerminalTextPrimary
                        )
                        Surface(
                            color = CommodoreAmber.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, CommodoreAmber.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "BOOK 1 • YEAR 1: SELF-REGULATION",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = CommodoreAmberBright,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = "25-Page Handwritten Storyboard & Cinematic Production Studio",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TerminalTextSecondary
                        )
                    }
                }

                // 5-Year EQ Pillar Strip
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NostromoRecessedWell, RoundedCornerShape(8.dp))
                        .border(1.dp, BulkheadBorder, RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "5-YEAR EMOTIONAL INTELLIGENCE SERIES ARCHITECTURE",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 10.sp,
                        color = NostromoTeal,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        EQ_FIVE_YEAR_PILLARS.forEach { pillar ->
                            Surface(
                                color = if (pillar.isActiveBook) CommodoreAmber else NostromoPanelCard,
                                border = BorderStroke(
                                    1.dp,
                                    if (pillar.isActiveBook) CommodoreAmberBright else BulkheadBorder
                                ),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = pillar.pillarTitle,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = if (pillar.isActiveBook) FontWeight.Bold else FontWeight.Normal,
                                    color = if (pillar.isActiveBook) NostromoVoidBg else TerminalTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Mandatory Visual Directive Warning Bulkhead
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HazardRose.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .border(1.dp, HazardRose.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                        .padding(12.dp),
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
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = HazardRose
                        )
                    }
                    Text(
                        text = "Permanent Face Obscuration: The hood-wearing 10-year-old protagonist never shows their face in any drawing. Through deliberate illustrative staging, the child's face is permanently hidden in every single panel—framed from behind, cast under deep hood shadow, eclipsed by massive tree trunks, or blocked by foreground environmental objects.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 12.sp,
                        color = TerminalTextPrimary
                    )
                }

                if (errorMessage != null) {
                    Surface(
                        color = HazardRose.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, HazardRose),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "TELEMETRY FAULT: $errorMessage",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = HazardRose,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Interactive Google Sign-In Button
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        onGoogleSignInClicked(
                            context = context,
                            credentialManager = credentialManager,
                            onAuthSuccess = {
                                isLoading = false
                                onAuthSuccess()
                            },
                            onAuthError = { msg ->
                                isLoading = false
                                errorMessage = msg
                            },
                            scope = coroutineScope,
                            onAuthCancelled = {
                                isLoading = false
                            }
                        )
                    },
                    enabled = !isLoading,
                    shape = CutCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CommodoreAmber,
                        contentColor = NostromoVoidBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("google_sign_in_button")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = NostromoVoidBg,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign in with Google // UNLOCK DECK",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                Text(
                    text = "CLOUD TELEMETRY: ALL 25 STORYBOARD WORKSHEETS & MULTI-LAYER SKETCHES SYNC AUTOMATICALLY TO YOUR SHIP LOG.",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 10.sp,
                    color = TerminalTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun NostromoRadarEmblem(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .background(NostromoRecessedWell, CutCornerShape(10.dp))
            .border(1.5.dp, FireflyCopper, CutCornerShape(10.dp))
    ) {
        val w = size.width
        val h = size.height
        // CRT grid
        for (i in 1..3) {
            val y = h * (i / 4f)
            val x = w * (i / 4f)
            drawLine(
                color = BulkheadBorder.copy(alpha = 0.6f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
            drawLine(
                color = BulkheadBorder.copy(alpha = 0.6f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
        }
        // Hooded 10-Yr-Old Dorsal Silhouette
        val hoodPath = Path().apply {
            moveTo(w * 0.36f, h * 0.22f)
            lineTo(w * 0.48f, h * 0.38f)
            lineTo(w * 0.52f, h * 0.80f)
            lineTo(w * 0.20f, h * 0.80f)
            lineTo(w * 0.24f, h * 0.38f)
            close()
        }
        drawPath(hoodPath, color = CommodoreAmber.copy(alpha = 0.28f))
        drawPath(hoodPath, color = CommodoreAmber, style = Stroke(width = 2.5f))

        // Tiny Pointy-Eared Chiweenie Silhouette
        val dogPath = Path().apply {
            moveTo(w * 0.58f, h * 0.66f)
            lineTo(w * 0.62f, h * 0.50f)
            lineTo(w * 0.67f, h * 0.62f)
            lineTo(w * 0.72f, h * 0.50f)
            lineTo(w * 0.76f, h * 0.65f)
            lineTo(w * 0.84f, h * 0.68f)
            lineTo(w * 0.84f, h * 0.80f)
            lineTo(w * 0.58f, h * 0.80f)
            close()
        }
        drawPath(dogPath, color = NostromoTeal.copy(alpha = 0.30f))
        drawPath(dogPath, color = NostromoTeal, style = Stroke(width = 2.2f))
    }
}
