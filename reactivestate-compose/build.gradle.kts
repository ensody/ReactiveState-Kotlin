import com.ensody.buildlogic.setupBuildLogic

plugins {
    id("com.ensody.build-logic")
}

setupBuildLogic {
    kotlin {
        sourceSets.commonMain.dependencies {
            api(compose.runtime)
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
