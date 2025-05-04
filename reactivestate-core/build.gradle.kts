import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic")
}

setupBuildLogic {
    kotlin {
        sourceSets.commonMain.dependencies {
            api(libs.coroutines.core)
        }
        sourceSets.commonTest.dependencies {
            implementation(project(":reactivestate-core-test"))
        }
    }
}
