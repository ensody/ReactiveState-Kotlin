dependencies {
    commonMainApi(project(":reactivestate-core"))
    commonMainApi(libs.kotlin.test.main)
    jvmCommonMainApi(libs.kotlin.test.junit)
    jvmCommonMainApi(libs.junit)
    commonMainApi(libs.coroutines.test)
    jvmCommonMainApi(libs.mockk)
}
