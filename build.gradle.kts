import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.3.31"
    kotlin("kapt") version "1.3.31"
    id("com.github.johnrengelman.shadow") version "5.0.0"
    id("flavor.pie.promptsign") version "1.1.0"
    id("maven-publish")
}

group = "flavor.pie"
version = "0.1.0"

repositories {
    mavenCentral()
    maven {
        name = "sponge"
        url = uri("https://repo.spongepowered.org/maven/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io/")
    }
    maven {
        name = "bstats"
        url = uri("https://repo.codemc.org/repository/maven-public")
    }
}

dependencies {
    val sponge = create(group = "org.spongepowered", name = "spongeapi", version = "7.1.0")
    api(sponge)
    kapt(sponge)
    val kotlin = kotlin("stdlib-jdk8")
    api(kotlin)
    shadow(kotlin)
    val kludge = create(group = "com.github.pie-flavor", name = "kludge", version = "477392a")
    implementation(kludge)
    shadow(kludge)
    val bstats = create(group = "org.bstats", name = "bstats-sponge-lite", version = "1.4")
    implementation(bstats)
    shadow(bstats)
}

tasks.named("jar") {
    enabled = false
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(project.configurations.shadow.get())
    archiveClassifier.set("")
    relocate("kotlin", "flavor.pie.laissezfaire.runtime.kotlin")
    relocate("flavor.pie.kludge", "flavor.pie.laissezfaire.util.kludge")
}

tasks.build {
    dependsOn(shadowJar)
}

tasks.named("signArchives") {
    dependsOn(shadowJar)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

publishing {
    publications {
        create("sponge", MavenPublication::class.java) {
            project.shadow.component(this)
            pom {
                name.set("LaissezFaire")
                description.set("A shop plugin")
                url.set("https://ore.spongepowered.org/pie_flavor/laissezfaire/")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://github.com/pie-flavor/LaissezFaire/blob/master/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("pie_flavor")
                        name.set("Adam Spofford")
                        email.set("aspofford.as@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/pie-flavor/LaissezFaire.git")
                    developerConnection.set("scm:git:ssh://github.com/pie-flavor/LaissezFaire.git")
                    url.set("https://github.com/pie-flavor/LaissezFaire/")
                }
            }
        }
        repositories {
            maven {
                url = uri(project.properties["spongePublishingUri"].toString())
                credentials {
                    username = project.properties["spongePublishingUsername"].toString()
                    password = project.properties["spongePublishingPassword"].toString()
                }
            }
        }
    }
}

