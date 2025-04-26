dependencies {
    commonMainApi(project(":reactivestate-android"))
    commonMainApi(project(":reactivestate-core-test"))

    androidMainApi(libs.androidx.arch.testing)
    androidMainApi(libs.androidx.fragment.testing)
    androidMainApi(libs.androidx.test.core)
    androidMainApi(libs.androidx.test.junit)
    androidMainApi(libs.robolectric)
}
