plugins {
  id("com.android.application")
}

// Dati dell'app, passati dalla compilazione GitHub (-PappId=... ecc.), letti da apps/ID.json.
fun prop(n: String, d: String) = (project.findProperty(n) as String?)?.takeIf { it.isNotBlank() } ?: d
val appId = prop("appId", "prova")
val appNome = prop("appNome", "App")
val appColore = prop("appColore", "#534AB7")
val appUrl = prop("appUrl", "")
val verCode = prop("verCode", "1").toInt()
val pacchetto = "com.dz.install." + appId.lowercase().replace(Regex("[^a-z0-9]"), "_")
    .let { if (it.first().isDigit()) "a$it" else it }
val coloreInt = "0xFF" + appColore.removePrefix("#").uppercase()
fun javaStr(s: String) = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
  namespace = "com.dz.install"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = pacchetto
    minSdk = 24
    targetSdk = 36
    versionCode = verCode
    versionName = "1.$verCode"
    resValue("string", "app_name", appNome.replace("'", "\\'"))
    resValue("color", "icona", appColore)
    buildConfigField("String", "NOME", javaStr(appNome))
    buildConfigField("String", "INSTALLER", javaStr(appUrl))
    buildConfigField("int", "COLORE", coloreInt)
  }

  buildFeatures {
    buildConfig = true
    resValues = true
  }

  // Chiave di firma fissa (Secrets del repo): ogni versione si installa sopra la precedente.
  val ks = System.getenv("KS_FILE")
  signingConfigs {
    create("fissa") {
      storeFile = file(ks ?: "assente.p12")
      storeType = "PKCS12"
      storePassword = System.getenv("KS_PASS")
      keyAlias = "appinstall"
      keyPassword = System.getenv("KS_PASS")
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = if (ks != null) signingConfigs.getByName("fissa") else signingConfigs.getByName("debug")
    }
  }

  lint {
    checkReleaseBuilds = false
    abortOnError = false
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}
