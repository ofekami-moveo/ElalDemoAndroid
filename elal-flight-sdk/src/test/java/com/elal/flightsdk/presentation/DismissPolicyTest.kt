package com.elal.flightsdk.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** No repost after the user swipes the Live Update away (task 4.3). */
class DismissPolicyTest {

    /** The dismissed journey is never posted again, whether by a push or by an in-flight tick (both go through this check). */
    @Test
    fun dismissedActivityIsNeverReposted() {
        assertFalse(DismissPolicy.shouldPost(activityId = "LY317-1110", dismissedActivityId = "LY317-1110"))
    }

    /** A new journey (another activityId) posts even though an older one was dismissed. */
    @Test
    fun newActivityIdPostsAfterDismiss() {
        assertTrue(DismissPolicy.shouldPost(activityId = "LY318-1111", dismissedActivityId = "LY317-1110"))
    }

    /** `end` clears the registry, so the next journey posts, even one reusing the same id (the demo uses ""). */
    @Test
    fun endClearsTheDismissal() {
        assertTrue(DismissPolicy.shouldPost(activityId = "", dismissedActivityId = null))
    }
}
