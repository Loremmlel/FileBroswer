package tenshi.hinanawi.filebrowser.util

import org.junit.Assume

internal fun skipTest(reason: String = "") {
    Assume.assumeFalse("跳过测试" + if (reason.isNotEmpty()) ": $reason" else "", true)
}