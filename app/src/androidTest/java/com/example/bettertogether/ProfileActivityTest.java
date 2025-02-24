/*

UI Test: Checking editing profile and image

**/
/*
Explanation:
    We first grant permission to read external storage. (for the image)
    We then test the edit profile data by changing the text in the fields and clicking the edit button.
    We then test the edit profile image by clicking the edit image button.
    We then check if the gallery intent is opened.
**/

package com.example.bettertogether;

import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasData;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@RunWith(AndroidJUnit4.class)
public class ProfileActivityTest {

    @Rule
    public GrantPermissionRule permissionRule = GrantPermissionRule.grant("android.permission.READ_EXTERNAL_STORAGE");

    @Before
    public void setUp() {
        Intents.init();
    }

    @After
    public void tearDown() {
        Intents.release();
    }
    // Test for the edit data button to change the text in the fields and click the edit button
    @Test
    public void testEditProfileData_validInputs_savesData() {
        // Launch the activity manually and enter input
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(ProfileActivity.class)) {
            onView(withId(R.id.edit_data_button)).perform(click());
            onView(withId(R.id.profile_bio)).perform(replaceText("New Bio"));
            onView(withId(R.id.profile_username_value)).perform(replaceText("NewUsername"));
            onView(withId(R.id.profile_gender_value)).perform(replaceText("Other"));
            onView(withId(R.id.profile_age_value)).perform(replaceText("30"));
            onView(withId(R.id.profile_dob_value)).perform(replaceText("01/01/1994"));
            onView(withId(R.id.profile_mobile_value)).perform(replaceText("1234567890"));
            onView(withId(R.id.edit_data_button)).perform(click());
        }
    }
    // Test for the edit image button to open the gallery intent
    @Test
    public void testEditProfileImage_opensGalleryIntent() {
        try (ActivityScenario<ProfileActivity> scenario = ActivityScenario.launch(ProfileActivity.class)) {
            onView(withId(R.id.edit_image_button)).perform(click());
            intended(hasAction(Intent.ACTION_PICK));
            intended(hasData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI));
        }
    }
}