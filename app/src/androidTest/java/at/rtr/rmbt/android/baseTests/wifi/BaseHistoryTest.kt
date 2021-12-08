/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package at.rtr.rmbt.android.baseTests.wifi

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import at.rtr.rmbt.android.R
import at.rtr.rmbt.android.baseTests.BaseHomeActivityTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
open class BaseHistoryTest : BaseHomeActivityTest() {

    @Before
    open fun setUp() {
        Espresso.onView(ViewMatchers.withId(R.id.navigation_history)).perform(ViewActions.click())
    }

    @Test
    fun checkTitleIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(R.id.labelTitle)).check(
            ViewAssertions.matches(ViewMatchers.isDisplayed())
        )
    }

    @Test
    fun checkSyncButtonIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(R.id.buttonSync)).check(
            ViewAssertions.matches(ViewMatchers.isDisplayed())
        )
    }

    @Test
    fun checkFilterButtonIsDisplayed() {
        Espresso.onView(ViewMatchers.withId(R.id.buttonMenu)).check(
            ViewAssertions.matches(ViewMatchers.isDisplayed())
        )
    }
}