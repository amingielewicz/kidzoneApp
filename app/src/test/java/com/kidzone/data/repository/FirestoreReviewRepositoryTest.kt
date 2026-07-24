@file:Suppress("WildcardImport")

package com.kidzone.data.repository

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.kidzone.data.local.ReviewDao
import com.kidzone.data.local.ReviewEntity
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.domain.model.Review
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import com.kidzone.utils.OpResult
import com.kidzone.utils.RepositoryException
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.net.UnknownHostException

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreReviewRepositoryTest {

    companion object {
        @JvmField
        @RegisterExtension
        val mainDispatcherRule = MainDispatcherRule()
    }

    private lateinit var firestore: FirebaseFirestore
    private lateinit var reviewDao: ReviewDao
    private lateinit var repository: FirestoreReviewRepository

    private lateinit var reviewsCollection: CollectionReference
    private lateinit var reviewReportsCollection: CollectionReference

    @BeforeEach
    fun setUp() {
        firestore = mockk(relaxed = true)
        reviewDao = mockk(relaxed = true)
        reviewsCollection = mockk(relaxed = true)
        reviewReportsCollection = mockk(relaxed = true)

        every { firestore.collection(FirestoreCollections.REVIEWS) } returns reviewsCollection
        every { firestore.collection(FirestoreCollections.REVIEW_REPORTS) } returns reviewReportsCollection

        repository = FirestoreReviewRepository(firestore, reviewDao)
    }

    // =========================================================================
    // addReview
    // =========================================================================

    @Nested
    @DisplayName("addReview()")
    inner class AddReview {

        @Test
        fun `rejects review with blank placeId`() = runTest {
            val review = TestFixtures.review(placeId = "")

            val result = repository.addReview(review)

            assertTrue(result is OpResult.Failure)
            val error = (result as OpResult.Failure).error
            assertTrue(error is IllegalArgumentException)
            assertTrue(error.message!!.contains("placeId"))
        }

        @Test
        fun `rejects review with rating below 1`() = runTest {
            val review = TestFixtures.review(rating = 0)

            val result = repository.addReview(review)

            assertTrue(result is OpResult.Failure)
            val error = (result as OpResult.Failure).error
            assertTrue(error is IllegalArgumentException)
            assertTrue(error.message!!.contains("rating"))
        }

        @Test
        fun `rejects review with rating above 5`() = runTest {
            val review = TestFixtures.review(rating = 6)

            val result = repository.addReview(review)

            assertTrue(result is OpResult.Failure)
            val error = (result as OpResult.Failure).error
            assertTrue(error is IllegalArgumentException)
        }

        @Test
        fun `rejects review with comment exceeding max length`() = runTest {
            val longComment = "a".repeat(1001)
            val review = TestFixtures.review(comment = longComment)

            val result = repository.addReview(review)

            assertTrue(result is OpResult.Failure)
            val error = (result as OpResult.Failure).error
            assertTrue(error is IllegalArgumentException)
            assertTrue(error.message!!.contains("limit"))
        }

        @Test
        fun `accepts review with comment at exactly max length`() = runTest {
            val exactComment = "a".repeat(500)
            val review = TestFixtures.review(
                id = "",
                comment = exactComment
            )

            val docRef = mockk<DocumentReference>(relaxed = true)
            every { docRef.id } returns "new-review-id"
            every { reviewsCollection.document() } returns docRef
            every { reviewsCollection.document("new-review-id") } returns docRef

            val task = mockk<Task<Void>>(relaxed = true)
            every { task.isComplete } returns true
            every { task.isSuccessful } returns true
            every { task.isCanceled } returns false
            every { task.exception } returns null
            every { task.result } returns null
            every { docRef.set(any()) } returns task

            val result = repository.addReview(review)

            assertTrue(result is OpResult.Success)
            val savedReview = (result as OpResult.Success).data
            assertEquals("new-review-id", savedReview.id)
            coVerify { reviewDao.upsert(any()) }
        }

        @Test
        fun `assigns generated id to review`() = runTest {
            val review = TestFixtures.review(id = "")

            val docRef = mockk<DocumentReference>(relaxed = true)
            every { docRef.id } returns "generated-id"
            every { reviewsCollection.document() } returns docRef
            every { reviewsCollection.document("generated-id") } returns docRef

            val task = mockk<Task<Void>>(relaxed = true)
            every { task.isComplete } returns true
            every { task.isSuccessful } returns true
            every { task.isCanceled } returns false
            every { task.exception } returns null
            every { task.result } returns null
            every { docRef.set(any()) } returns task

            val result = repository.addReview(review)

            assertTrue(result is OpResult.Success)
            assertEquals("generated-id", (result as OpResult.Success).data.id)
        }

        @Test
        fun `persists to local cache on success`() = runTest {
            val review = TestFixtures.review(id = "")

            val docRef = mockk<DocumentReference>(relaxed = true)
            every { docRef.id } returns "new-id"
            every { reviewsCollection.document() } returns docRef
            every { reviewsCollection.document("new-id") } returns docRef

            val task = mockk<Task<Void>>(relaxed = true)
            every { task.isComplete } returns true
            every { task.isSuccessful } returns true
            every { task.isCanceled } returns false
            every { task.exception } returns null
            every { task.result } returns null
            every { docRef.set(any()) } returns task

            repository.addReview(review)

            coVerify { reviewDao.upsert(match { it.id == "new-id" }) }
        }

        @Test
        fun `network failure does not enqueue optimistic offline add`() = runTest {
            val review = TestFixtures.review()

            val docRef = mockk<DocumentReference>(relaxed = true)
            every { docRef.id } returns "new-id"
            every { reviewsCollection.document() } returns docRef
            every { reviewsCollection.document("new-id") } returns docRef
            every { docRef.set(any()) } throws UnknownHostException("offline")

            val result = repository.addReview(review)

            assertTrue(result is OpResult.Failure)
            coVerify(exactly = 0) { reviewDao.upsert(any()) }
        }
    }

    // =========================================================================
    // updateReview
    // =========================================================================

    @Nested
    @DisplayName("updateReview()")
    inner class UpdateReview {

        @Test
        fun `rejects review with blank id`() = runTest {
            val review = TestFixtures.review(id = "")

            val result = repository.updateReview(review)

            assertTrue(result is OpResult.Failure)
            val error = (result as OpResult.Failure).error
            assertTrue(error is IllegalArgumentException)
            assertTrue(error.message!!.contains("id"))
        }

        @Test
        fun `rejects review with invalid rating`() = runTest {
            val review = TestFixtures.review(id = "r1", rating = 0)

            val result = repository.updateReview(review)

            assertTrue(result is OpResult.Failure)
        }

        @Test
        fun `rejects review with comment over limit`() = runTest {
            val review = TestFixtures.review(id = "r1", comment = "x".repeat(1001))

            val result = repository.updateReview(review)

            assertTrue(result is OpResult.Failure)
        }

        @Test
        fun `successful update sets updatedAtMillis`() = runTest {
            val review = TestFixtures.review(id = "r1", rating = 4, comment = "Updated")

            val docRef = mockk<DocumentReference>(relaxed = true)
            every { reviewsCollection.document("r1") } returns docRef

            val task = mockk<Task<Void>>(relaxed = true)
            every { task.isComplete } returns true
            every { task.isSuccessful } returns true
            every { task.isCanceled } returns false
            every { task.exception } returns null
            every { task.result } returns null
            every { docRef.set(any()) } returns task

            val result = repository.updateReview(review)

            assertTrue(result is OpResult.Success)
            val updated = (result as OpResult.Success).data
            assertTrue(updated.updatedAtMillis > 0)
            coVerify { reviewDao.upsert(any()) }
        }

        @Test
        fun `network failure does not enqueue optimistic offline update`() = runTest {
            val review = TestFixtures.review(id = "r1", rating = 4, comment = "Updated")

            val docRef = mockk<DocumentReference>(relaxed = true)
            every { reviewsCollection.document("r1") } returns docRef
            every { docRef.set(any()) } throws UnknownHostException("offline")

            val result = repository.updateReview(review)

            assertTrue(result is OpResult.Failure)
            coVerify(exactly = 0) { reviewDao.upsert(any()) }
        }
    }

    // =========================================================================
    // deleteReview
    // =========================================================================

    @Nested
    @DisplayName("deleteReview()")
    inner class DeleteReview {

        @Test
        fun `rejects blank reviewId`() = runTest {
            val result = repository.deleteReview("")

            assertTrue(result is OpResult.Failure)
            val error = (result as OpResult.Failure).error
            assertTrue(error is IllegalArgumentException)
        }

        @Test
        fun `successful deletion removes from cache`() = runTest {
            val docRef = mockk<DocumentReference>(relaxed = true)
            every { reviewsCollection.document("r1") } returns docRef

            val task = mockk<Task<Void>>(relaxed = true)
            every { task.isComplete } returns true
            every { task.isSuccessful } returns true
            every { task.isCanceled } returns false
            every { task.exception } returns null
            every { task.result } returns null
            every { docRef.delete() } returns task

            val result = repository.deleteReview("r1")

            assertTrue(result is OpResult.Success)
            coVerify { reviewDao.deleteById("r1") }
        }

        @Test
        fun `network failure does not enqueue optimistic offline delete`() = runTest {
            val docRef = mockk<DocumentReference>(relaxed = true)
            every { reviewsCollection.document("r1") } returns docRef
            every { docRef.delete() } throws UnknownHostException("offline")

            val result = repository.deleteReview("r1")

            assertTrue(result is OpResult.Failure)
            coVerify(exactly = 0) { reviewDao.deleteById(any()) }
        }
    }

    // =========================================================================
    // reportReviewAsSpam
    // =========================================================================

    @Nested
    @DisplayName("reportReviewAsSpam()")
    inner class ReportReview {

        @Test
        fun `rejects blank reviewId`() = runTest {
            val result = repository.reportReviewAsSpam(
                reviewId = "", reporterId = "user-1", reason = "SPAM", comment = ""
            )

            assertTrue(result is OpResult.Failure)
        }

        @Test
        fun `rejects blank reporterId`() = runTest {
            val result = repository.reportReviewAsSpam(
                reviewId = "r1", reporterId = "", reason = "SPAM", comment = ""
            )

            assertTrue(result is OpResult.Failure)
        }

        @Test
        fun `rejects duplicate report from same user`() = runTest {
            // Simulate existing report found
            val queryRef = mockk<Query>(relaxed = true)
            val querySnapshot = mockk<QuerySnapshot>(relaxed = true)
            val existingDoc = mockk<DocumentSnapshot>(relaxed = true)

            every { reviewReportsCollection.whereEqualTo("reporterId", "user-1") } returns queryRef
            every { queryRef.whereEqualTo("reviewId", "r1") } returns queryRef

            val task = mockk<Task<QuerySnapshot>>(relaxed = true)
            every { task.isComplete } returns true
            every { task.isSuccessful } returns true
            every { task.isCanceled } returns false
            every { task.exception } returns null
            every { task.result } returns querySnapshot
            every { queryRef.get() } returns task
            every { querySnapshot.documents } returns listOf(existingDoc)

            val result = repository.reportReviewAsSpam(
                reviewId = "r1", reporterId = "user-1", reason = "SPAM", comment = "test"
            )

            assertTrue(result is OpResult.Failure)
            val error = (result as OpResult.Failure).error
            assertTrue(error is RepositoryException.AlreadyReported)
        }
    }

    // =========================================================================
    // getReportedReviews
    // =========================================================================

    @Nested
    @DisplayName("getReportedReviews()")
    inner class GetReported {

        @Test
        fun `returns empty set on error`() = runTest {
            every { reviewReportsCollection.whereEqualTo("reporterId", "user-1") } throws
                RuntimeException("Network error")

            val result = repository.getReportedReviews("user-1")

            assertTrue(result.isEmpty())
        }
    }
}
