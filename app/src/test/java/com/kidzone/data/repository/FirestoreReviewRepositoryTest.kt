package com.kidzone.data.repository

import com.google.firebase.firestore.*
import com.kidzone.data.local.ReviewDao
import com.kidzone.data.local.ReviewEntity
import com.kidzone.data.remote.FirestoreCollections
import com.kidzone.domain.model.Review
import com.kidzone.testutil.MainDispatcherRule
import com.kidzone.testutil.TestFixtures
import com.kidzone.utils.OpResult
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.google.android.gms.tasks.Task
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

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
            val exactComment = "a".repeat(1000)
            val review = TestFixtures.review(comment = exactComment)

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
            val review = TestFixtures.review()

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
            assertTrue(error is IllegalStateException)
            assertTrue(error.message!!.contains("Już zgłosiłeś"))
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
