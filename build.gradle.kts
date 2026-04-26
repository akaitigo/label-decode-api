plugins {
  kotlin("jvm") version "2.3.20"
  kotlin("plugin.allopen") version "2.3.20"
  id("io.quarkus") version "3.35.0"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
  id("com.diffplug.spotless") version "7.0.3"
}

repositories { mavenCentral() }

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
  implementation(
      enforcedPlatform(
          "$quarkusPlatformGroupId:$quarkusPlatformArtifactId:$quarkusPlatformVersion"))
  implementation("io.quarkus:quarkus-kotlin")
  implementation("io.quarkus:quarkus-arc")
  implementation("io.quarkus:quarkus-grpc")
  implementation("io.quarkus:quarkus-jdbc-postgresql")
  implementation("io.quarkus:quarkus-hibernate-orm-panache-kotlin")
  implementation("io.quarkus:quarkus-flyway")
  implementation("org.jetbrains.kotlin:kotlin-stdlib")
  implementation("com.google.protobuf:protobuf-kotlin:4.34.0")

  testImplementation("io.quarkus:quarkus-junit5")
  testImplementation("io.quarkus:quarkus-jdbc-h2")
  testImplementation("io.rest-assured:rest-assured")
}

group = "com.akaitigo"

version = "0.0.1"

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
  compilerOptions {
    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    javaParameters.set(true)
  }
}

allOpen {
  annotation("jakarta.ws.rs.Path")
  annotation("jakarta.enterprise.context.ApplicationScoped")
  annotation("jakarta.persistence.Entity")
  annotation("io.quarkus.test.junit.QuarkusTest")
}

detekt {
  config.setFrom("$projectDir/detekt.yml")
  parallel = true
  buildUponDefaultConfig = true
}

spotless {
  kotlin {
    target("src/**/*.kt")
    ktfmt()
  }
  kotlinGradle { ktfmt() }
}
