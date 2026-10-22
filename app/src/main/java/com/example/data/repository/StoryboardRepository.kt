package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.OperationType
import com.example.data.handleFirestoreError
import com.example.data.model.StoryboardPage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StoryboardRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    // CRITICAL: Always initialize with R.string.firestore_database_id
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun pageDocId(pageNumber: Int): String {
        val clamped = pageNumber.coerceIn(1, 25)
        return "page_${clamped.toString().padStart(2, '0')}"
    }

    fun observeUserPages(userId: String? = null): Flow<List<StoryboardPage>> = flow {
        val uid = userId ?: auth.currentUser?.uid ?: "unauthenticated"
        val path = "users/$uid/pages"
        emitAll(
            db.collection("users")
                .document(uid)
                .collection("pages")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        StoryboardPage::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val unwrapped = (error as? CancellationException)?.cause as? Exception
                        ?: (error as? CancellationException)?.cause?.cause as? Exception
                        ?: (error as? Exception)
                    if (unwrapped != null) {
                        handleFirestoreError(unwrapped, OperationType.LIST, path)
                        throw unwrapped
                    }
                    throw error
                }
        )
    }

    suspend fun getUserPages(userId: String? = null): Result<List<StoryboardPage>> {
        val uid = userId ?: requireUserId()
        val path = "users/$uid/pages"
        return try {
            val snapshot = db.collection("users")
                .document(uid)
                .collection("pages")
                .whereEqualTo("userId", uid)
                .get()
                .await()
            val pages = snapshot.toObjects(
                StoryboardPage::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(pages)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, path)
            Result.failure(e)
        }
    }

    suspend fun getPageById(
        pageId: String,
        targetUserId: String? = null
    ): Result<StoryboardPage?> {
        val uid = targetUserId ?: requireUserId()
        val docRef = db.collection("users").document(uid).collection("pages").document(pageId)
        return try {
            val snapshot = docRef.get().await()
            val page = snapshot.toObject(
                StoryboardPage::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(page)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun savePage(page: StoryboardPage): Result<String> {
        val uid = requireUserId()
        val clampedPage = page.pageNumber.coerceIn(1, 25)
        val docId = page.id.ifBlank { pageDocId(clampedPage) }
        val docRef = db.collection("users").document(uid).collection("pages").document(docId)
        val isUpdate = page.createdAt != null
        val sanitized = page.copy(
            id = docId,
            userId = uid,
            pageNumber = clampedPage,
            mandate = page.mandate.take(300),
            cameraDistance = page.cameraDistance.take(100),
            separationForce = page.separationForce.take(100),
            storyline = page.storyline.take(5000),
            illustration = page.illustration.take(5000),
            kidPosture = page.kidPosture.take(1000),
            dogAction = page.dogAction.take(1000),
            distanceApart = page.distanceApart.take(500),
            trigger = page.trigger.take(1000),
            response = page.response.take(1000),
            turningPoint = page.turningPoint.take(1000),
            terrain = page.terrain.take(2000),
            obstacle = page.obstacle.take(2000),
            sketchStrokes = page.sketchStrokes.take(200000),
            createdAt = if (isUpdate) page.createdAt else null,
            updatedAt = null
        )

        return try {
            docRef.set(sanitized).await()
            Result.success(docId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(
                e,
                if (isUpdate) OperationType.UPDATE else OperationType.CREATE,
                docRef.path
            )
            Result.failure(e)
        }
    }

    suspend fun deletePage(pageNumber: Int): Result<Unit> {
        val uid = requireUserId()
        val docId = pageDocId(pageNumber)
        val docRef = db.collection("users").document(uid).collection("pages").document(docId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
