package org.tensorflow.lite.examples.textclassification

import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Tests of [TextClassificationClient]  */
@RunWith(AndroidJUnit4::class)
class UnitTest {

    private var client: TextClassificationClient? = null

    @Before
    fun setUp() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        client = TextClassificationClient(appContext)
        client!!.load()
    }

    @Test
    fun loadModelTest() {
        Assert.assertNotNull(client!!.classifier)
    }

    @Test
    fun predictTest() {
        val positiveText = client!!.classify("This is an interesting film. My family and I all liked it very much.")[0]
        Assert.assertEquals("Positive", positiveText.title)
        Assert.assertTrue(positiveText.confidence!! > 0.55)
        val negativeText = client!!.classify("This film cannot be worse. It is way too boring.")[0]
        Assert.assertEquals("Negative", negativeText.title)
        Assert.assertTrue(negativeText.confidence!! > 0.6)
    }
}