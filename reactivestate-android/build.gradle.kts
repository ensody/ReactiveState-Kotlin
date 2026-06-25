import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic.androidlib")
    id("com.ensody.build-logic.kmp")
    id("com.ensody.build-logic.publish")
}

setupBuildLogic {
    kotlin {
        sourceSets.androidMain.dependencies {
            api(libs.androidx.annotation)
            api(libs.androidx.appcompat)
            api(libs.androidx.core)
            api(libs.androidx.activity)
            api(libs.androidx.fragment)

            api(libs.androidx.lifecycle.runtime)
            api(libs.androidx.lifecycle.livedata)
            api(libs.androidx.lifecycle.service)
//            api(libs.androidx.lifecycle.viewmodel)
        }
        sourceSets["androidHostTest"].dependencies {
            implementation(libs.androidx.arch.testing)
            implementation(libs.androidx.fragment.testing)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.junit)
            implementation(libs.robolectric)
        }
        sourceSets.commonMain.dependencies {
            api(project(":reactivestate-core"))
        }
        sourceSets.commonTest.dependencies {
            implementation(project(":reactivestate-android-test"))
        }
    }
}
