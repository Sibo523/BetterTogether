/**
 *
 * UI Test: Verifying filling a new form and submitting
 *
 */
/**
 *
 * Explanation:
 *      NewRoomActivityUITest.java simulates a user filling out the New Room form by typing text into various fields,
 *      selecting a date via the DatePicker (using PickerActions),
 *      adding poll options, selecting a radio button, and then clicking the submit button.
 *      After a brief wait for asynchronous operations to complete,
 *      the test checks (using Espresso Intents) that an intent was fired to navigate to RoomActivity.
 *
 */

package com.example.bettertogether;
import androidx.test.espresso.contrib.PickerActions;
import androidx.test.espresso.intent.Intents;
import androidx.test.espresso.intent.matcher.IntentMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import android.widget.DatePicker;
import com.example.bettertogether.NewRoomActivity;
import com.example.bettertogether.RoomActivity;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class NewRoomActivityUITest {
    @Rule
    public ActivityTestRule<NewRoomActivity> activityRule =
            new ActivityTestRule<>(NewRoomActivity.class);

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }

    @Test
    public void testSubmitForm_validInputs_navigatesToRoomActivity() {
        // Enter room name (assumed id: room_name)
        onView(withId(R.id.room_name))
                .perform(typeText("Test Room"), closeSoftKeyboard());

        // Enter bet amount (assumed id: form_number_input)
        onView(withId(R.id.form_number_input))
                .perform(typeText("100"), closeSoftKeyboard());

        // Enter max participants (assumed id: max_participants_input)
        onView(withId(R.id.max_participants_input))
                .perform(typeText("10"), closeSoftKeyboard());

        // Open the date picker by clicking the date input (assumed id: form_date_input)
        onView(withId(R.id.form_date_input)).perform(click());
        // Set the date to October 10, 2025 using Espresso’s PickerActions
        onView(withClassName(Matchers.equalTo(DatePicker.class.getName())))
                .perform(PickerActions.setDate(2025, 10, 10));
        // Confirm the date selection (usually the positive button has id android.R.id.button1)
        onView(withId(android.R.id.button1)).perform(click());

        // Enter description (assumed id: form_description_input)
        onView(withId(R.id.form_description_input))
                .perform(typeText("A valid description"), closeSoftKeyboard());

        // Ensure public checkbox is checked (assumed id: form_checkbox_public)
        onView(withId(R.id.form_checkbox_public))
                .perform(click());

        // Add poll option "Option 1" (assumed ids: poll_option_input and add_poll_option_button)
        onView(withId(R.id.poll_option_input))
                .perform(typeText("Option 1"), closeSoftKeyboard());
        onView(withId(R.id.add_poll_option_button))
                .perform(click());

        // Add poll option "Option 2"
        onView(withId(R.id.poll_option_input))
                .perform(replaceText("Option 2"), closeSoftKeyboard());
        onView(withId(R.id.add_poll_option_button))
                .perform(click());

        // Select a radio button (assumed id: radio_option1)
        onView(withId(R.id.radio_option1)).perform(click());

        // Click submit button (assumed id: submit_button)
        onView(withId(R.id.submit_button)).perform(click());

        // Not recommended for production tests but can be used for debugging.
        try {
            Thread.sleep(5000); // wait 5 seconds for async tasks to complete
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify that after submission, the app navigates to RoomActivity
        intended(IntentMatchers.hasComponent(RoomActivity.class.getName()));
    }
}
