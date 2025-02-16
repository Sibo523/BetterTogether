package com.example.bettertogether

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.bettertogether.RatingActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
class RatingActivityUnitTest {

    private lateinit var activity: RatingActivity
    private lateinit var mockFirestore: FirebaseFirestore
    private lateinit var mockCollection: CollectionReference
    private lateinit var mockQuery: Query

    @Before
    fun setup() {
        // Build the activity using Robolectric
        activity = Robolectric.buildActivity(RatingActivity::class.java).create().get()

        // Create mocks for the Firestore query chain
        mockFirestore = Mockito.mock(FirebaseFirestore::class.java)
        mockCollection = Mockito.mock(CollectionReference::class.java)
        mockQuery = Mockito.mock(Query::class.java)

        Mockito.`when`(mockFirestore.collection("users")).thenReturn(mockCollection)
        Mockito.`when`(
            mockCollection.orderBy(
                Mockito.eq("currentPoints"),
                Mockito.any()
            )
        ).thenReturn(mockQuery)
        Mockito.`when`(mockQuery.limit(20)).thenReturn(mockQuery)

        // Inject our mock Firestore instance into the activity’s db property.
        // (Assuming 'db' is accessible or defined in BaseActivity.)
        activity.db = mockFirestore
    }

    @Test
    fun testLoadTopUsers_success() {
        // Create a fake DocumentSnapshot
        val fakeDoc = Mockito.mock(DocumentSnapshot::class.java)
        Mockito.`when`(fakeDoc.getString("displayName")).thenReturn("User One")
        Mockito.`when`(fakeDoc.getString("role")).thenReturn("participant")
        Mockito.`when`(fakeDoc.getString("photoUrl")).thenReturn("http://example.com/img.jpg")
        Mockito.`when`(fakeDoc.getLong("currentPoints")).thenReturn(1000L)

        // Create a fake QuerySnapshot with one document
        val fakeQuerySnapshot = Mockito.mock(QuerySnapshot::class.java)
        Mockito.`when`(fakeQuerySnapshot.documents).thenReturn(listOf(fakeDoc))

        // Return a successful Task from get()
        val fakeTask: Task<QuerySnapshot> = Tasks.forResult(fakeQuerySnapshot)
        Mockito.`when`(mockQuery.get()).thenReturn(fakeTask)

        // Call loadTopUsers; the task completes immediately.
        activity.loadTopUsers()

        // Verify that the topUsersList has been updated with one document.
        assertEquals("Expected one user in topUsersList", 1, activity.topUsersList.size)
    }

    @Test
    fun testLoadTopUsers_failure() {
        // Create a failed Task simulating a Firestore query error.
        val exception = Exception("Test failure")
        val failedTask: Task<QuerySnapshot> = Tasks.forException(exception)
        Mockito.`when`(mockQuery.get()).thenReturn(failedTask)

        // Call loadTopUsers
        activity.loadTopUsers()

        // Use Robolectric’s ShadowToast to verify a toast was shown with the error message.
        val latestToast = ShadowToast.getTextOfLatestToast()
        assertTrue("Expected toast with error message", latestToast.contains("Error fetching top users:"))
    }
}
