import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic.kmp")
    id("com.ensody.build-logic.compose")
    id("com.ensody.build-logic.publish")
}

setupBuildLogic {
    kotlin {
        sourceSets.commonMain.dependencies {
            api(compose.runtime)
            api(libs.androidx.lifecycle.runtime.compose)
            api(project(":reactivestate-core"))
        }
        sourceSets["composeMain"].dependencies {
            api(compose.foundation)
            api(compose.ui)
            api(libs.androidx.lifecycle.viewmodel.compose)
        }
        sourceSets.androidUnitTest.dependencies {
            implementation(project(":reactivestate-android-test"))
        }
    }

    dependencies {
        implementation(platform(libs.compose.bom))
    }
}
