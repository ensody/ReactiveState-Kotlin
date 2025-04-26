dependencies {
    androidMainApi(libs.androidx.annotation)
    androidMainApi(libs.androidx.appcompat)
    androidMainApi(libs.androidx.core)
    androidMainApi(libs.androidx.activity)
    androidMainApi(libs.androidx.fragment)

    androidMainApi(libs.androidx.lifecycle.runtime)
    androidMainApi(libs.androidx.lifecycle.livedata)
    androidMainApi(libs.androidx.lifecycle.service)
//    androidMainApi(libs.androidx.lifecycle.viewmodel)
    constraints {
        api(libs.androidx.lifecycle.common)
        api(libs.androidx.lifecycle.service)
        api(libs.androidx.lifecycle.process)
    }
    commonMainApi(project(":reactivestate-core"))
    commonTestImplementation(project(":reactivestate-android-test"))

    androidTestImplementation(libs.androidx.arch.testing)
    androidTestImplementation(libs.androidx.fragment.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.robolectric)
}
