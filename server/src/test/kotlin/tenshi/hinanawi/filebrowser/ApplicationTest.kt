package tenshi.hinanawi.filebrowser

import org.junit.runner.RunWith
import org.junit.runners.Suite
import tenshi.hinanawi.filebrowser.plugin.PathValidatorTest
import tenshi.hinanawi.filebrowser.route.FilesEndpointTest
import tenshi.hinanawi.filebrowser.route.ImageEndpointTest
import tenshi.hinanawi.filebrowser.route.RandomEndpointTest

@RunWith(Suite::class)
@Suite.SuiteClasses(
  RouteTest::class,
  PluginTest::class,
  ServiceTest::class
)
class ApplicationTest

@RunWith(Suite::class)
@Suite.SuiteClasses(
  FilesEndpointTest::class,
  RandomEndpointTest::class,
  ImageEndpointTest::class
)
class RouteTest

@RunWith(Suite::class)
@Suite.SuiteClasses(
  PathValidatorTest::class
)
class PluginTest

@RunWith(Suite::class)
@Suite.SuiteClasses(
)
class ServiceTest