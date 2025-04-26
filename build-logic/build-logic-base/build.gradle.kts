plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

dependencies {
    api(libs.android.gradle)
    api(libs.kotlin.gradle)
    api(libs.dokka.gradle)
    api(libs.gradleVersions)
}

gradlePlugin {
    plugins {
        create("com.ensody.build-logic-base") {
            id = name
            implementationClass = "com.ensody.buildlogic.BuildLogicBasePlugin"
        }
    }
}
