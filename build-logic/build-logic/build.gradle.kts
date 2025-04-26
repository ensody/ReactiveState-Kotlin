plugins {
    `kotlin-dsl`
    id("java-gradle-plugin")
}

dependencies {
    api(project(":build-logic-base"))
    api(libs.nexus.gradle)
//    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))
}

gradlePlugin {
    plugins {
        create("com.ensody.build-logic") {
            id = name
            implementationClass = "com.ensody.buildlogic.BuildLogicPlugin"
        }
    }
}
