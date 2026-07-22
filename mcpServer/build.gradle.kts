import java.util.Properties

plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        load(f.inputStream())
    }
}

tasks.named<JavaExec>("run") {
    environment("OPENAI_API_KEY", localProps.getProperty("OPENAI_API_KEY", ""))
    environment("CENSUS_API_KEY", localProps.getProperty("CENSUS_API_KEY", ""))

}

dependencies {
    // the frozen universal backend, the MCP server is just a new consumer of it
    implementation(projects.core)

    // the official Kotlin MCP SDK (server + client)
    implementation(libs.mcp.kotlin.sdk)

    // for runBlocking / the suspend server handlers
    implementation(libs.kotlinx.coroutines.core)

    // for pulling string args out of tool call JSONObjbect
    implementation(libs.kotlinx.serialization.json)

    // RAG pipeline for the ask tool
    implementation(projects.rag)

    testImplementation(libs.kotlin.testJunit)
}

application {
    // points at the Main.kt
    mainClass.set("com.app.backendplug_kmp.mcp.MainKt")
}

tasks.named<Test>("test") {
    dependsOn("installDist")
}