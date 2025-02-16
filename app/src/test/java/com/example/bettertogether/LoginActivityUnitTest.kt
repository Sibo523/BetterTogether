package com.example.bettertogether

import com.example.bettertogether.LoginActivity
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class LoginActivityUnitTest {
    @Mock
    lateinit var mockAuth: FirebaseAuth

    private lateinit var loginActivity: LoginActivity

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        // Use a spy so we can verify calls to methods such as goToMainScreen()
        loginActivity = Mockito.spy(LoginActivity())
        loginActivity.auth = mockAuth
    }

    @Test
    fun testFirebaseAuthWithGoogle_successfulSignIn_callsGoToMainScreen() {
        // Create a fake successful Task for signInWithCredential
        val fakeAuthResult = Mockito.mock(AuthResult::class.java)
        val successfulTask: Task<AuthResult> = Tasks.forResult(fakeAuthResult)

        // When signInWithCredential is called with any credential, return our successful task
        Mockito.`when`(mockAuth.signInWithCredential(Mockito.any())).thenReturn(successfulTask)

        // Call the function under test with a dummy token
        loginActivity.firebaseAuthWithGoogle("dummyToken")

        // Verify that after a successful sign-in, goToMainScreen() is called
        // (Note: This requires that goToMainScreen() is open and spy-able)
        Mockito.verify(loginActivity, Mockito.times(1)).goToMainScreen()
    }

}