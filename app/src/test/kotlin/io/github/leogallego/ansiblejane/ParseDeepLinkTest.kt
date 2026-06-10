package io.github.leogallego.ansiblejane

import io.github.leogallego.ansiblejane.navigation.ApprovalDetailRoute
import io.github.leogallego.ansiblejane.navigation.JobStatusRoute
import io.github.leogallego.ansiblejane.navigation.WorkflowJobStatusRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParseDeepLinkTest {

    @Test
    fun `valid approval deep link returns ApprovalDetailRoute`() {
        val result = MainActivity.parseDeepLink("ansiblejane://approval/42")
        assertEquals(ApprovalDetailRoute(42), result)
    }

    @Test
    fun `valid job deep link returns JobStatusRoute`() {
        val result = MainActivity.parseDeepLink("ansiblejane://job/100")
        assertEquals(JobStatusRoute(100), result)
    }

    @Test
    fun `valid workflow deep link returns WorkflowJobStatusRoute`() {
        val result = MainActivity.parseDeepLink("ansiblejane://workflow/7")
        assertEquals(WorkflowJobStatusRoute(7), result)
    }

    @Test
    fun `wrong scheme returns null`() {
        assertNull(MainActivity.parseDeepLink("https://approval/42"))
        assertNull(MainActivity.parseDeepLink("myapp://approval/42"))
    }

    @Test
    fun `missing ID returns null`() {
        assertNull(MainActivity.parseDeepLink("ansiblejane://approval"))
        assertNull(MainActivity.parseDeepLink("ansiblejane://approval/"))
    }

    @Test
    fun `non-numeric ID returns null`() {
        assertNull(MainActivity.parseDeepLink("ansiblejane://approval/abc"))
    }

    @Test
    fun `negative ID returns null`() {
        assertNull(MainActivity.parseDeepLink("ansiblejane://approval/-1"))
    }

    @Test
    fun `zero ID returns null`() {
        assertNull(MainActivity.parseDeepLink("ansiblejane://approval/0"))
    }

    @Test
    fun `path traversal returns null`() {
        assertNull(MainActivity.parseDeepLink("ansiblejane://approval/../etc/passwd"))
        assertNull(MainActivity.parseDeepLink("ansiblejane://../approval/42"))
    }

    @Test
    fun `double slash returns null`() {
        assertNull(MainActivity.parseDeepLink("ansiblejane://approval//42"))
    }

    @Test
    fun `unknown type returns null`() {
        assertNull(MainActivity.parseDeepLink("ansiblejane://settings/1"))
        assertNull(MainActivity.parseDeepLink("ansiblejane://unknown/42"))
    }

    @Test
    fun `empty URI returns null`() {
        assertNull(MainActivity.parseDeepLink(""))
    }
}
