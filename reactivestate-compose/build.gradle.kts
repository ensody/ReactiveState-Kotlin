plugins {
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

dependencies {
    implementation(platform(libs.compose.bom))

    commonMainApi(compose.runtime)
    composeMainApi(compose.foundation)
    composeMainApi(compose.ui)
    composeMainApi(libs.androidx.lifecycle.viewmodel.compose)

    commonMainApi(project(":reactivestate-core"))
    jvmTestImplementation(project(":reactivestate-android-test"))
}
