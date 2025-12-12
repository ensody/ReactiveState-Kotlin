import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic.android")
    id("com.ensody.build-logic.kmp")
    id("com.ensody.build-logic.publish")
}

setupBuildLogic {
    kotlin {
        sourceSets.commonMain.dependencies {
            api(project(":reactivestate-core"))
            api(libs.kotlin.test.main)
            api(libs.coroutines.test)
        }
    }
}
