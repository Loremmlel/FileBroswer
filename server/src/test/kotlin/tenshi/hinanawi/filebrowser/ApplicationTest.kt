package tenshi.hinanawi.filebrowser

import org.junit.runner.RunWith
import org.junit.runners.Suite
import tenshi.hinanawi.filebrowser.plugin.PathValidatorTest
import tenshi.hinanawi.filebrowser.route.FilesEndpointTest
import tenshi.hinanawi.filebrowser.route.RandomEndpointTest

@RunWith(Suite::class)
@Suite.SuiteClasses(
  FilesEndpointTest::class,
  RandomEndpointTest::class,
  PathValidatorTest::class
)
class ApplicationTest