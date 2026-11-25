package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun app_name_matches_strata() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("STRATA", context.getString(R.string.app_name))
    }

    @Test
    fun cuj_discover_search_open_player_and_switch_depth_crans() {
        composeRule.waitForIdle()
        // Verify initial catalog track is displayed
        composeRule.onNodeWithText("Abidjan Minuit").assertIsDisplayed()

        // Search by BPM
        composeRule.onNodeWithTag("search_input").performTextInput("96")
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Soleil de Bassam").assertIsDisplayed()

        // Tap track card -> expands studio player
        composeRule.onNodeWithTag("track_card_trk_04_soleil_bassam").performClick()
        composeRule.waitForIdle()

        // Switch depth slider to Cran 3 (Pistes)
        composeRule.onNodeWithTag("depth_slider_cran_3").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("karaoke_mode_btn").assertIsDisplayed()

        // Switch depth slider to Cran 4 (Studio)
        composeRule.onNodeWithTag("depth_slider_cran_4").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("create_fork_btn").assertIsDisplayed()
    }

    @Test
    fun cuj_navigate_to_orchestra_and_artist_ledger() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_orchestra").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("orchestra_create_btn").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("orchestra_play_btn").assertIsDisplayed()

        composeRule.onNodeWithTag("nav_artist").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("artist_tab_ledger").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("payout_mobile_money_btn").assertIsDisplayed()
    }
}
