plugins {
	kotlin("jvm") version "2.3.21"
	kotlin("plugin.spring") version "2.3.21"
	id("org.springframework.boot") version "4.1.3"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.shilan"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

extra["springAiVersion"] = "2.0.2"

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-jdbc")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.ai:spring-ai-starter-mcp-server-webmvc")
	implementation("tools.jackson.module:jackson-module-kotlin")
	implementation("org.springframework.ai:spring-ai-starter-model-google-genai")
	runtimeOnly("net.snowflake:snowflake-jdbc:4.4.1")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-jdbc-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("com.h2database:h2")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict", "-Xemit-jvm-type-annotations")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
	testLogging {
		events("failed", "skipped")
		exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
	}
	// Gradle prints nothing on success; one summary line makes the count visible locally and in CI logs.
	// Gradle prints nothing on success; one summary line makes the count visible locally and in CI logs.
	addTestListener(object : TestListener {
		override fun beforeSuite(suite: TestDescriptor) {}
		override fun beforeTest(testDescriptor: TestDescriptor) {}
		override fun afterTest(testDescriptor: TestDescriptor, result: TestResult) {}
		override fun afterSuite(suite: TestDescriptor, result: TestResult) {
			if (suite.parent == null) {
				println("Tests: ${result.testCount} run, ${result.successfulTestCount} passed, " +
						"${result.failedTestCount} failed, ${result.skippedTestCount} skipped")
			}
		}
	})
}

tasks.test {
	// The Google GenAI client resolves ADC at startup and CI has no GCP credentials, so the
	// real chat model is off for unit tests; tests that need a model supply a fake ChatModel.
	systemProperty("spring.ai.model.chat", "none")
	useJUnitPlatform { excludeTags("live") }
}

// Evals against live Gemini on Vertex AI, run locally with ADC; never part of ./gradlew test or CI.
tasks.register<Test>("liveEval") {
	description = "Runs the live-model evals (JUnit tag 'live') against Gemini on Vertex AI."
	group = "verification"
	testClassesDirs = sourceSets["test"].output.classesDirs
	classpath = sourceSets["test"].runtimeClasspath
	useJUnitPlatform { includeTags("live") }
	// Model answers can change between runs, so a previous result is never up to date.
	outputs.upToDateWhen { false }
	testLogging { showStandardStreams = true }
}
