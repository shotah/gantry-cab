import java.util.Properties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) {
  localFile.inputStream().use { localProps.load(it) }
}

val envFileProps = Properties()
val envFile = rootProject.file(".env")
if (envFile.exists()) {
  envFile.inputStream().use { envFileProps.load(it) }
}

fun esc(value: String): String = "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

fun cabVersionName(): String {
  val raw = rootProject.file("VERSION").readText().trim()
  val name = raw.removePrefix("v")
  require(name.matches(Regex("""\d+\.\d+\.\d+"""))) { "VERSION must be vMAJOR.MINOR.PATCH, got $raw" }
  return name
}

fun cabVersionCode(name: String): Int {
  val parts = name.split(".")
  require(parts.size == 3)
  val minor = parts[1].toInt()
  val patch = parts[2].toInt()
  require(minor in 0..99 && patch in 0..99) {
    "VERSION minor and patch must be 0-99 (versionCode is major*10000+minor*100+patch), got $name"
  }
  return parts[0].toInt() * 10_000 + minor * 100 + patch
}

val cabName = cabVersionName()
val cabCode = cabVersionCode(cabName)

fun propOrEnv(key: String, env: String): String? =
  System.getenv(env)?.takeIf { it.isNotBlank() }
    ?: envFileProps.getProperty(env)?.takeIf { it.isNotBlank() }
    ?: envFileProps.getProperty(key)?.takeIf { it.isNotBlank() }
    ?: localProps.getProperty(key)?.takeIf { it.isNotBlank() }

val mailboxOrigin = propOrEnv("cab.mailboxOrigin", "CAB_MAILBOX_ORIGIN") ?: "http://10.0.2.2:3000"
val googleWebClientId = propOrEnv("cab.googleWebClientId", "CAB_GOOGLE_WEB_CLIENT_ID") ?: ""

val playStore = propOrEnv("cab.storeFile", "CAB_STORE_FILE")
val playStorePassword = propOrEnv("cab.storePassword", "CAB_STORE_PASSWORD")
val playKeyAlias = propOrEnv("cab.keyAlias", "CAB_KEY_ALIAS")
val playKeyPassword = propOrEnv("cab.keyPassword", "CAB_KEY_PASSWORD")
val hasPlayKey = listOf(playStore, playStorePassword, playKeyAlias, playKeyPassword).all { it != null }

android {
  namespace = "com.gantree.cab"
  compileSdk = 37

  defaultConfig {
    applicationId = "com.gantree.cab"
    minSdk = 28
    targetSdk = 37
    versionCode = cabCode
    versionName = cabName
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "MAILBOX_ORIGIN", esc(mailboxOrigin))
    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", esc(googleWebClientId))
    buildConfigField("boolean", "DEV", "false")
  }

  signingConfigs {
    if (hasPlayKey) {
      create("play") {
        storeFile = file(checkNotNull(playStore))
        storePassword = checkNotNull(playStorePassword)
        keyAlias = checkNotNull(playKeyAlias)
        keyPassword = checkNotNull(playKeyPassword)
      }
    }
  }

  buildTypes {
    debug {
      enableUnitTestCoverage = true
      isDebuggable = true
      buildConfigField("boolean", "DEV", "true")
    }
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // GitHub Release sideload is the product. Play Store is not a goal.
      // Optional CAB_KEYSTORE_* keeps one SHA-1 for Google Sign-In.
      signingConfig = if (hasPlayKey) {
        signingConfigs.getByName("play")
      } else {
        signingConfigs.getByName("debug")
      }
    }
  }

  lint {
    abortOnError = true
    checkReleaseBuilds = false
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2026.08.00")
  implementation(composeBom)
  implementation("androidx.fragment:fragment-ktx:1.8.8")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.activity:activity-compose:1.13.0")
  implementation("androidx.activity:activity-ktx:1.13.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.11.0")
  implementation("androidx.lifecycle:lifecycle-service:2.11.0")
  implementation("androidx.core:core-ktx:1.19.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("androidx.car.app:app:1.7.0")
  implementation("androidx.car.app:app-projected:1.7.0")
  implementation("androidx.credentials:credentials:1.6.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
  implementation("com.google.android.gms:play-services-location:21.4.0")
  implementation("com.mikepenz:multiplatform-markdown-renderer:0.43.0")
  implementation("com.mikepenz:multiplatform-markdown-renderer-m3:0.43.0")
  implementation("io.coil-kt.coil3:coil-compose:3.6.2")
  implementation("io.coil-kt.coil3:coil-network-okhttp:3.6.2")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.json:json:20240303")
  testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
  testImplementation("org.kordamp.ikonli:ikonli-swing:12.4.0")
  testImplementation("org.kordamp.ikonli:ikonli-material2-pack:12.4.0")
  testImplementation("org.kordamp.ikonli:ikonli-fontawesome6-pack:12.4.0")
  debugImplementation("androidx.compose.ui:ui-tooling")
}

tasks.withType<Test>().configureEach {
  reports.html.required.set(false)
}
