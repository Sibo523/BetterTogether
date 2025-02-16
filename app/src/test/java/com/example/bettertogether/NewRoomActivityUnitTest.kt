package com.example.bettertogether

import android.widget.RadioButton
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.bettertogether.NewRoomActivity
import com.google.ar.core.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class NewRoomActivityUnitTest {
    private lateinit var activity: NewRoomActivity

    @Before
    fun setup() {
        // Build the activity using Robolectric
        activity = Robolectric.buildActivity(NewRoomActivity::class.java).create().get()
    }

    @Test
    fun testValidateInputs_validData_returnsTrue() {
        // Setting up valid conditions
        activity.roomNameText.setText("Test Room")
        activity.betAmountInput.setText("100")
        activity.maxParticipantsInput.setText("10")
        activity.dateInput.setText("10/10/2025")
        activity.descriptionInput.setText("A valid description")
        activity.checkbox_public.isChecked = true  // For a public room, code input is not required

        // Simulate a selected radio button in the radio group
        val radioButton = RadioButton(activity)
        radioButton.id = 1234
        radioButton.text = "Ratio Option"
        activity.radioGroup.addView(radioButton)
        activity.radioGroup.check(radioButton.id)

        // Ensure we have at least 2 poll options
        activity.pollOptions.clear()
        activity.pollOptions.add("Option 1")
        activity.pollOptions.add("Option 2")

        activity.checkbox_event.isChecked = false  // Not an event, so no event subject is needed

        // Call the function under test
        val result = activity.validateInputs()

        // Assert that valid inputs return true
        assertTrue("Expected validateInputs() to return true for valid data", result)
    }

    @Test
    fun testValidateInputs_emptyRoomName_returnsFalse() {
        // Setting up invalid conditions (empty room name)
        activity.roomNameText.setText("")
        activity.betAmountInput.setText("100")
        activity.maxParticipantsInput.setText("10")
        activity.dateInput.setText("10/10/2025")
        activity.descriptionInput.setText("A valid description")
        activity.checkbox_public.isChecked = true

        // Simulate a radio button selection
        val radioButton = RadioButton(activity)
        radioButton.id = 1234
        radioButton.text = "Ratio Option"
        activity.radioGroup.addView(radioButton)
        activity.radioGroup.check(radioButton.id)

        // Provide valid poll options
        activity.pollOptions.clear()
        activity.pollOptions.add("Option 1")
        activity.pollOptions.add("Option 2")
        activity.checkbox_event.isChecked = false

        // Call the function under test
        val result = activity.validateInputs()

        // Assert that an empty room name causes validation to fail
        assertFalse("Expected validateInputs() to return false for empty room name", result)
    }
}