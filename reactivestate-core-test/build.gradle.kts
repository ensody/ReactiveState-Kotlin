import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic")
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
