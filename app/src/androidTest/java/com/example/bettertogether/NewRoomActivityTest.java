/**
 *
 * UI Test: Verifying visibilities of the code input
 *
 */
/**
 *
 * Explanation:
 *      NewRoomActivityTest.java launches the activity and verifies that the code input is initially visible.
 *      When the public checkbox is clicked, it then confirms that the code input is hidden.
 *
 */


package com.example.bettertogether;
import static androidx.test.espresso.Espresso.onView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.example.bettertogether.NewRoomActivity;
import com.example.bettertogether.R;

@RunWith(AndroidJUnit4.class)
public class NewRoomActivityTest {

    @Test
    public void testVisibilityOfCodeInputWhenCheckboxChecked() {
        // Launch the activity manually
        ActivityScenario<NewRoomActivity> scenario = ActivityScenario.launch(NewRoomActivity.class);

        // Check visibility
        onView(ViewMatchers.withId(R.id.form_code_input))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()));

        // Click the checkbox
        onView(ViewMatchers.withId(R.id.form_checkbox_public))
                .perform(ViewActions.click());

        // Check visibility again
        onView(ViewMatchers.withId(R.id.form_code_input))
                .check(ViewAssertions.matches(ViewMatchers.withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }
}
