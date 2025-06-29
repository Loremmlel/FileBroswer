plugins {
  alias(libs.plugins.kotlinJvm)
  alias(libs.plugins.ktor)
  application
}

group = "tenshi.hinanawi.filebrowser"
version = "1.0.0"
application {
  mainClass.set("tenshi.hinanawi.filebrowser.ApplicationKt")

  val isDevelopment: Boolean = project.ext.has("development")
  applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
  implementation(projects.shared)
  implementation(libs.logback)
  implementation(libs.ktor.serverCore)
  implementation(libs.ktor.serverNetty)
  implementation(libs.ktor.contentNegotiation)
  implementation(libs.ktor.serializationKotlinxJson)
  implementation(libs.ktor.serverCors)
  testImplementation(libs.ktor.serverTestHost)
  testImplementation(libs.kotlin.testJunit)
}

// 自定义测试任务
tasks.register("apiTest") {
  group = "verification"
  description = "🚀 运行所有API测试 - 一键测试所有端点"

  dependsOn("test")

  val reportPath = layout.buildDirectory.file("reports/tests/test/index.html")

  doLast {
    println("🎉 所有API测试完成！")
    println("📊 查看详细报告: file://${reportPath.get().asFile.absolutePath}")
  }
}

// 详细测试任务（用于调试）
tasks.register<Test>("apiTestVerbose") {
  group = "verification"
  description = "🔍 运行所有API测试 - 显示详细调试信息"

  // 配置详细的测试日志
  testLogging {
    events("passed", "skipped", "failed", "standardOut", "standardError")
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    showExceptions = true
    showCauses = true
    showStackTraces = true
    showStandardStreams = true
  }

  // 不使用自定义日志配置，显示所有信息
  jvmArgs("-Dio.ktor.development=false")

  val reportPath = layout.buildDirectory.file("reports/tests/test/index.html")

  doFirst {
    println("🔍 开始运行详细API测试...")
    println("⚠️  注意：此模式会显示所有调试信息，输出较多")
  }

  doLast {
    println("🔍 详细测试完成！")
    println("📊 查看详细报告: file://${reportPath.get().asFile.absolutePath}")
  }
}

// 增强默认测试任务
tasks.test {
  testLogging {
    // 只显示重要的测试事件，过滤掉内部调试信息
    events("passed", "skipped", "failed")

    // 只在测试失败时显示详细信息
    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.SHORT
    showExceptions = true
    showCauses = false
    showStackTraces = false

    // 过滤掉标准输出中的调试信息
    showStandardStreams = false
  }

  // 设置JVM参数，降低Ktor测试框架的日志级别
  jvmArgs(
    "-Dlogback.configurationFile=src/test/resources/logback-test.xml",
    "-Dio.ktor.development=false"
  )

  val reportPath = layout.buildDirectory.file("reports/tests/test/index.html")

  doFirst {
    println("🚀 开始运行 FileBrowser API 测试套件...")
    println("📝 运行测试中，请稍候...")
  }

  doLast {
    println("✅ 测试完成！")
    println("📊 测试报告: file://${reportPath.get().asFile.absolutePath}")
  }
}
