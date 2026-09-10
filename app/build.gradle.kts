import java.util.Properties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

val localProps = Properties()
val localFile = rootProject.file("local.properties")
if (localFile.exists()) {
  localFile.inputStream().use { localProps.load(it) }
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
  return parts[0].toInt() * 10_000 + parts[1].toInt() * 100 + parts[2].toInt()
}

val cabName = cabVersionName()
val cabCode = cabVersionCode(cabName)

fun propOrEnv(key: String, env: String): String? =
  System.getenv(env)?.takeIf { it.isNotBlank() } ?: localProps.getProperty(key)?.takeIf { it.isNotBlank() }

val playStore = propOrEnv("cab.storeFile", "CAB_STORE_FILE")
val playStorePassword = propOrEnv("cab.storePassword", "CAB_STORE_PASSWORD")
val playKeyAlias = propOrEnv("cab.keyAlias", "CAB_KEY_ALIAS")
val playKeyPassword = propOrEnv("cab.keyPassword", "CAB_KEY_PASSWORD")
val hasPlayKey = listOf(playStore, playStorePassword, playKeyAlias, playKeyPassword).all { it != null }

android {
  namespace = "com.gantree.cab"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.gantree.cab"
    minSdk = 28
    targetSdk = 35
    versionCode = cabCode
    versionName = cabName
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    buildConfigField("String", "MAILBOX_ORIGIN", esc(localProps.getProperty("cab.mailboxOrigin", "http://10.0.2.2:3000")))
    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", esc(localProps.getProperty("cab.googleWebClientId", "")))
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
    }
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      // Sideload / GitHub Release until a Play key is in local.properties or CI secrets.
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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  kotlinOptions {
    jvmTarget = "17"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }
}

dependencies {
  val composeBom = platform("androidx.compose:compose-bom:2025.01.00")
  implementation(composeBom)
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.activity:activity-compose:1.10.0")
  implementation("androidx.activity:activity-ktx:1.10.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
  implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
  implementation("androidx.lifecycle:lifecycle-service:2.8.7")
  implementation("androidx.core:core-ktx:1.15.0")
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("androidx.car.app:app:1.7.0")
  implementation("androidx.car.app:app-projected:1.7.0")
  implementation("androidx.credentials:credentials:1.3.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
  implementation("com.google.android.gms:play-services-location:21.3.0")
  testImplementation("junit:junit:4.13.2")
  testImplementation("org.json:json:20240303")
  debugImplementation("androidx.compose.ui:ui-tooling")
}
