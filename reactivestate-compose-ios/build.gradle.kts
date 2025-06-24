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
            api(project(":reactivestate-compose"))
        }
    }
}
