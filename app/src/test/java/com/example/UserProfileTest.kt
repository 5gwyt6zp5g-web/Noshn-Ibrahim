package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.EducationalResourceEntity
import com.example.data.UserEntity
import com.example.ui.components.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class UserProfileTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun userProfile_displaysUserInfo_andEmptyResourcesState() {
    val mockUser = UserEntity(
      id = "user_123",
      name = "Alice Lovelace",
      email = "alice@u.edu",
      role = "INSTRUCTOR",
      earnings = 250.0,
      premiumUser = true
    )

    var ctaClicked = false

    composeTestRule.setContent {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        UserProfile(
          user = mockUser,
          uploadedResources = emptyList(),
          isFirestoreLoading = false,
          onResourceClick = {},
          onRefreshFirestore = {},
          onGoToStudioTab = { ctaClicked = true }
        )
      }
    }

    // Verify Alice's info and elements are rendered
    composeTestRule.onNodeWithTag("profile_summary_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("premium_badge").assertIsDisplayed()
    composeTestRule.onNodeWithTag("empty_uploads_card").performScrollTo().assertIsDisplayed()

    // Clicking the Studio CTA triggers action
    composeTestRule.onNodeWithTag("blank_uploads_cta").performScrollTo().performClick()
    assertEquals(true, ctaClicked)
  }

  @Test
  fun userProfile_displaysUploadedResourcesList() {
    val mockUser = UserEntity(
      id = "user_456",
      name = "Bob Turing",
      email = "bob@u.edu",
      role = "STUDENT",
      earnings = 0.0,
      premiumUser = false
    )

    val mockResources = listOf(
      EducationalResourceEntity(
        id = "res_quantum",
        title = "Quantum Computing Handbook",
        description = "Introductory lecture series notes",
        price = 12.99,
        category = "Computer Science",
        authorId = "user_456",
        authorName = "Bob Turing"
      )
    )

    var clickedResource: EducationalResourceEntity? = null

    composeTestRule.setContent {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        UserProfile(
          user = mockUser,
          uploadedResources = mockResources,
          isFirestoreLoading = false,
          onResourceClick = { clickedResource = it },
          onRefreshFirestore = {},
          onGoToStudioTab = {}
        )
      }
    }

    // Verify Bob's profile
    composeTestRule.onNodeWithTag("profile_summary_card").assertIsDisplayed()
    // Since premiumUser is false, check premium_badge is NOT shown
    composeTestRule.onNodeWithTag("premium_badge").assertDoesNotExist()

    // Ensure "My Uploads" tab is selected
    composeTestRule.onNodeWithTag("profile_tab_0").performClick()

    // Verify uploaded educational resources list and item render correctly
    composeTestRule.onNodeWithTag("uploaded_resources_list").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("uploaded_resource_item_res_quantum").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("copy_uploaded_resource_link_btn_res_quantum").assertIsDisplayed()

    // Test list item click registration
    composeTestRule.onNodeWithTag("uploaded_resource_item_res_quantum").performScrollTo().performClick()
    assertEquals("res_quantum", clickedResource?.id)
  }

  @Test
  fun userProfile_displaysWishlistedItems_andExportButton() {
    val mockUser = UserEntity(
      id = "user_123",
      name = "Alice Lovelace",
      email = "alice@u.edu",
      role = "STUDENT",
      earnings = 0.0,
      premiumUser = false
    )

    val mockCourses = listOf(
      com.example.data.CourseEntity(
        id = "course_ai2",
        title = "Introduction to Deep Learning",
        description = "A great deep learning course",
        instructorId = "prof_x",
        instructorName = "Prof. X",
        price = 49.99,
        category = "AI",
        lessonsJson = "[]"
      )
    )

    val mockNotes = listOf(
      com.example.data.NoteEntity(
        id = "note_stats2",
        title = "Probability & Statistics Cheatsheet",
        description = "Formulas for midterm",
        price = 2.99,
        category = "Mathematics",
        previewText = "Formulas...",
        downloadUrl = "url_stats2",
        sellerId = "user_456",
        sellerName = "Bob Turing"
      )
    )

    composeTestRule.setContent {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        UserProfile(
          user = mockUser,
          uploadedResources = emptyList(),
          isFirestoreLoading = false,
          onResourceClick = {},
          onRefreshFirestore = {},
          onGoToStudioTab = {},
          wishlistedCourses = mockCourses,
          wishlistedNotes = mockNotes
        )
      }
    }

    // Ensure "Favorites" tab is selected
    composeTestRule.onNodeWithTag("profile_tab_2").performClick()

    // Verify wishlist section items are displayed
    composeTestRule.onNodeWithTag("wishlist_items_list").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("wishlist_course_item_course_ai2").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("wishlist_note_item_note_stats2").performScrollTo().assertIsDisplayed()

    // Verify Export PDF button is displayed
    composeTestRule.onNodeWithTag("export_favorites_pdf_btn").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun userProfile_displaysPurchasedResources_withVisualProgressTracker() {
    val mockUser = UserEntity(
      id = "user_123",
      name = "Alice Lovelace",
      email = "alice@u.edu",
      role = "STUDENT",
      earnings = 0.0,
      premiumUser = false
    )

    val mockPurchasedResources = listOf(
      EducationalResourceEntity(
        id = "res_quantum",
        title = "Quantum Computing Handbook",
        description = "Introductory lecture series notes",
        price = 12.99,
        category = "Computer Science",
        authorId = "user_456",
        authorName = "Bob Turing"
      )
    )

    val mockProgress = listOf(
      com.example.data.ResourceProgressEntity(
        userId = "user_123",
        resourceId = "res_quantum",
        completedPagesCsv = "0,2" // 2 out of 4 sections completed
      )
    )

    val mockPurchases = listOf(
      com.example.data.PurchaseEntity(
        id = "purchase_1",
        userId = "user_123",
        contentId = "res_quantum",
        contentType = "RESOURCE",
        pricePaid = 12.99,
        timestamp = System.currentTimeMillis()
      )
    )

    composeTestRule.setContent {
      Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        UserProfile(
          user = mockUser,
          uploadedResources = emptyList(),
          isFirestoreLoading = false,
          onResourceClick = {},
          onRefreshFirestore = {},
          onGoToStudioTab = {},
          purchasedResources = mockPurchasedResources,
          resourceProgress = mockProgress,
          allPurchases = mockPurchases
        )
      }
    }

    // Ensure "Purchased" tab is selected
    composeTestRule.onNodeWithTag("profile_tab_1").performClick()

    // Scroll to and verify purchased resource rendering
    composeTestRule.onNodeWithTag("purchased_resource_item_res_quantum").performScrollTo().assertIsDisplayed()
    
    // Verify progress tracking nodes and copy link button are visible
    composeTestRule.onNodeWithTag("resource_progress_bar_res_quantum").assertIsDisplayed()
    composeTestRule.onNodeWithTag("copy_purchased_resource_link_btn_res_quantum").assertIsDisplayed()
  }
}
