plugins {
    kotlin("jvm") version "2.0.0"
    `maven-publish`
}

group = "com.fidluc"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

kotlin {
    jvmToolchain(17)
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("run") {
    group = "application"
    description = "Runs the Main demonstration generating and reading sample Excel spreadsheets"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.fidluc.sheetstream.MainKt")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.fidluc"
            artifactId = "sheetstream"
            version = "1.0.0"

            pom {
                name.set("SheetStream")
                description.set("Zero-dependency, O(1) memory, streaming Excel (.xlsx) generator for Kotlin & Android")
                url.set("https://github.com/sheetstream/sheetstream")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
            }
        }
    }
}

