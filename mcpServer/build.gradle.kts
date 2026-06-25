plugins {
    alias(libs.plugins.kotlinJvm)
    application
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