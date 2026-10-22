package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.SketchCanvasState
import com.example.data.model.SketchLayer
import com.example.data.model.SketchPoint
import com.example.data.model.SketchStroke
import com.example.data.model.StoryboardPage
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class StoryboardRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun savePage_validPayloadWithMultiLayerSketch_createsAndUpdatesDocument() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = StoryboardRepository(firestore, auth)
        val uniqueDocId = "page_${UUID.randomUUID().toString().take(8)}"

        val canvasState = SketchCanvasState(
            layers = listOf(
                SketchLayer(
                    id = "layer_1",
                    name = "L1 // TERRAIN",
                    visible = true,
                    strokes = listOf(
                        SketchStroke(
                            colorHex = "#F59E0B",
                            strokeWidth = 3f,
                            points = listOf(SketchPoint(100f, 120f), SketchPoint(240f, 180f))
                        )
                    )
                )
            ),
            activeLayerId = "layer_1"
        )
        val serializedSketch = SketchCanvasState.serialize(canvasState)

        val newPage = StoryboardPage.createBlank(1, aliceUid).copy(
            id = uniqueDocId,
            completed = true,
            ruleDorsal = true,
            ruleHood = true,
            cameraDistance = "Wide Env.",
            separationForce = "Nature",
            storyline = "Page 01: The wind howls through the obsidian pines.",
            illustration = "Dorsal angle behind hooded 10-yr-old watching the tiny Chiweenie.",
            sketchStrokes = serializedSketch
        )

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.savePage(newPage)
        }
        assertTrue("Expected savePage create to succeed", createResult.isSuccess)
        val docId = createResult.getOrThrow()
        assertEquals(uniqueDocId, docId)

        // Read back the created page so createdAt is populated
        val fetchedPage = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getPageById(uniqueDocId, aliceUid).getOrThrow()
        }
        assertNotNull("Expected fetched page not null", fetchedPage)
        assertEquals("Page 01: The wind howls through the obsidian pines.", fetchedPage!!.storyline)

        // Now test updating the existing page (verifies createdAt preservation & affectedKeys rule)
        val updatedPage = fetchedPage.copy(
            storyline = "Page 01 Revised: Rain drums against the hood.",
            trigger = "Sudden flash flood in the ravine"
        )
        val updateResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.savePage(updatedPage)
        }
        assertTrue("Expected savePage update to succeed", updateResult.isSuccess)
    }

    @Test
    fun observeUserPages_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = StoryboardRepository(firestore, auth)
        val uniqueDocId = "page_${UUID.randomUUID().toString().take(8)}"

        val page2 = StoryboardPage.createBlank(2, aliceUid).copy(
            id = uniqueDocId,
            storyline = "Page 02: Tracking small pawprints in the mud."
        )
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.savePage(page2).getOrThrow()
        }

        val emitted = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeUserPages(aliceUid).first { list ->
                list.any { it.id == uniqueDocId }
            }
        }
        assertTrue(emitted.any { it.id == uniqueDocId && it.storyline.contains("pawprints") })
    }

    @Test
    fun getPageById_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = StoryboardRepository(firestore, auth)
        val uniqueDocId = "page_${UUID.randomUUID().toString().take(8)}"

        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.savePage(
                StoryboardPage.createBlank(3, aliceUid).copy(id = uniqueDocId)
            ).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = StoryboardRepository(firestore, auth)
        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getPageById(uniqueDocId, targetUserId = aliceUid)
        }
        assertTrue("Expected cross-user read to fail", result.isFailure)
        val ex = result.exceptionOrNull() as? FirebaseFirestoreException
        assertNotNull("Expected FirebaseFirestoreException", ex)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, ex!!.code)
    }

    @Test
    fun observeUserPages_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repository = StoryboardRepository(firestore, auth)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repository.observeUserPages("unauthenticated_target").first()
            }
            fail("Expected PERMISSION_DENIED exception")
        } catch (t: Throwable) {
            val firestoreEx = generateSequence(t) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            assertNotNull("Expected underlying FirebaseFirestoreException, got $t", firestoreEx)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx!!.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice_okuz@test.com"
        const val BOB_EMAIL = "bob_okuz@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
