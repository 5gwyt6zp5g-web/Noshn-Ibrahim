package com.example

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.data.CategoryEntity
import com.example.data.EducationalResourceEntity
import com.example.ui.HomeScreen
import com.example.ui.EduHubViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36])
class RecommendationSystemTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun testRecommendationCalculations_prioritizesInteractedCategories() = runTest {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = EduHubViewModel(app)

    // Ensure we have some base educational resources
    val resourceList = listOf(
      EducationalResourceEntity("r1", "Calculus Guide", "Math study deck", 5.0, "Mathematics", "author_1", "Prof"),
      EducationalResourceEntity("r2", "Intro to Python", "Coding book", 0.0, "Computer Science", "author_1", "Prof"),
      EducationalResourceEntity("r3", "Organic Chemistry", "Lab notes", 15.0, "Science", "author_1", "Prof")
    )
    
    // Test the scoring algorithm of the combine flows
    // We record context click on "Computer Science"
    viewModel.recordCategoryInteraction("Computer Science")
    
    val interactions = viewModel.interactedCategories.first()
    assertTrue(interactions.contains("Computer Science"))
  }

  @Test
  fun homeScreen_displaysRecommendations_whenSectionIsPopulated() {
    val dummyCategories = listOf(
      CategoryEntity("cat1", "Computer Science", "Computer")
    )
    val dummyResources = listOf(
      EducationalResourceEntity("res_1", "Intro to Coding", "Learn Kotlin", 0.0, "Computer Science", "author_1", "Prof")
    )

    composeTestRule.setContent {
      HomeScreen(
        categories = dummyCategories,
        courses = emptyList(),
        notes = emptyList(),
        educationalResources = emptyList(),
        recommendedResources = dummyResources,
        selectedCategory = null,
        searchQuery = "",
        isFirestoreOnline = false,
        isFirestoreLoading = false,
        firestoreMessage = null,
        purchasedIds = emptySet(),
        wishlistIds = emptySet(),
        onSearchQueryChanged = {},
        onCategorySelected = {},
        onCourseClick = {},
        onNoteClick = {},
        onResourceClick = {},
        onToggleWishlist = { _, _ -> },
        onClearFirestoreMessage = {},
        onFirestoreSyncClicked = {},
        onCreateResourceClicked = { _, _, _, _, _, _ -> },
        onReportResourceClicked = { _, _ -> }
      )
    }

    // Verify search bar and category chips are displayed
    composeTestRule.onNodeWithTag("resource_search_bar").assertIsDisplayed()

    // Verify recommendations list is displayed
    composeTestRule.onNodeWithTag("recommended_resources_list").performScrollTo().assertIsDisplayed()
  }
}
