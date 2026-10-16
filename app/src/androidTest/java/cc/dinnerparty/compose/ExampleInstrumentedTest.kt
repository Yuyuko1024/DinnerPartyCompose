package cc.dinnerparty.compose

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/** Android 设备上的插桩测试示例。 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // 被测应用的 Context
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("cc.dinnerparty.compose", appContext.packageName)
    }
}