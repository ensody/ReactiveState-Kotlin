import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic.android")
    id("com.ensody.build-logic.kmp")
    id("com.ensody.build-logic.publish")
}

setupBuildLogic {
    kotlin {
        sourceSets.commonMain.dependencies {
            api(project(":reactivestate-android"))
            api(project(":reactivestate-core-test"))
        }
        sourceSets.androidMain.dependencies {
            api(libs.androidx.arch.testing)
            api(libs.androidx.fragment.testing)
            api(libs.androidx.test.core)
            api(libs.androidx.test.junit)
            api(libs.robolectric)

            api(libs.kotlin.test.junit)
            api(libs.junit)
            api(libs.mockk)
        }
    }
}
