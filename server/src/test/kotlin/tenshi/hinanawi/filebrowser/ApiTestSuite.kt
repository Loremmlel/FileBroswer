package tenshi.hinanawi.filebrowser

import org.junit.platform.suite.api.SelectClasses
import org.junit.platform.suite.api.Suite
import org.junit.platform.suite.api.SuiteDisplayName
import tenshi.hinanawi.filebrowser.tests.FilesEndpointTest
import tenshi.hinanawi.filebrowser.tests.RandomEndpointTest

@Suite
@SuiteDisplayName("Hinanawi FileBrowser API Test Suite")
@SelectClasses(
    FilesEndpointTest::class,
    RandomEndpointTest::class
)
class ApiTestSuite