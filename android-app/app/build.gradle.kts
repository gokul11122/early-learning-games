import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// ---------------------------------------------------------------------------
// Things that must NEVER change once the app is on someone's phone.
// Changing any of them makes Android treat the new APK as a different app:
// it refuses to update, and uninstalling the old one wipes the children's progress.
//   1. applicationId below
//   2. the signing key (signing.properties + the .jks file it points to)
//   3. the WebView origin in MainActivity (https://appassets.androidplatform.net)
// ---------------------------------------------------------------------------
val appId = "io.github.gokul11122.earlylearning"

// Every build gets a bigger versionCode than the one before (minutes since 2024-01-01 UTC),
// so a new APK always installs as an update over the old one. No number to remember to bump.
val buildTime: Instant = Instant.now()
val buildVersionCode = ((buildTime.epochSecond - 1_704_067_200L) / 60L).toInt()
val buildVersionName: String = DateTimeFormatter.ofPattern("yyyy.MM.dd.HHmm")
    .withZone(ZoneOffset.UTC).format(buildTime)

// The release key lives OUTSIDE the repo. Override the location with EARLY_LEARNING_SIGNING.
val signingFile = file(
    System.getenv("EARLY_LEARNING_SIGNING")
        ?: "${System.getProperty("user.home")}/.android-keys/early-learning-games/signing.properties"
)
val signingProps = Properties().apply { if (signingFile.exists()) signingFile.inputStream().use { load(it) } }

android {
    namespace = appId
    compileSdk = 35

    defaultConfig {
        applicationId = appId
        minSdk = 24
        targetSdk = 35
        versionCode = buildVersionCode
        versionName = buildVersionName
    }

    signingConfigs {
        if (signingFile.exists()) {
            create("release") {
                storeFile = signingFile.resolveSibling(
                    requireNotNull(signingProps.getProperty("storeFile")) { "storeFile missing in $signingFile" }
                )
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        // Debug builds install as a separate app ("...earlylearning.debug"), so trying one on a family
        // phone can never clash with — or tempt an uninstall of — the real app and its progress.
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
            // Testing only (./gradlew assembleRelease -PinspectableRelease): a release build whose
            // WebView can be inspected from chrome://inspect. Never share an APK built this way.
            isDebuggable = providers.gradleProperty("inspectableRelease").isPresent
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // The games are NOT copied into this project by hand: every build packs the current
    // index.html + games/ from the repo root, so the APK always matches the website.
    sourceSets["main"].assets.srcDir(layout.buildDirectory.dir("generated/webassets"))
}

val syncWebAssets by tasks.registering(Sync::class) {
    from(rootProject.projectDir.parentFile) {
        include("index.html", "games/**")
    }
    into(layout.buildDirectory.dir("generated/webassets"))
}
tasks.named("preBuild") { dependsOn(syncWebAssets) }

// Refuse to build an unsigned or wrongly-signed release: it could never update the installed app.
tasks.matching { it.name == "packageRelease" }.configureEach {
    doFirst {
        if (!signingFile.exists()) {
            throw GradleException(
                "No release signing key at $signingFile. Run ./build-apk.sh once to create it, " +
                    "or restore your backup of ~/.android-keys/early-learning-games/."
            )
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.12.1")
}
