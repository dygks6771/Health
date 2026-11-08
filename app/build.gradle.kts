import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * 출시용 서명 정보. 저장소에 올리지 않는 keystore.properties (프로젝트 루트) 에서 읽는다.
 *   storeFile=C:/Users/.../runners-chronicle.jks
 *   storePassword=...
 *   keyAlias=runners
 *   keyPassword=...
 * 파일이 없으면 release 빌드는 서명되지 않은 APK/AAB 로 만들어진다.
 */
val keystoreProps = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

/** 출시 빌드가 접속할 서버 (https 필수). gradle.properties 의 releaseServerUrl 로 바꾼다 */
val releaseServerUrl: String = providers.gradleProperty("releaseServerUrl").getOrElse("https://example.com")

android {
    namespace = "com.example.health"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        // 스토어에 한 번 올리면 바꿀 수 없는 앱 고유 ID (com.example 은 스토어 등록 불가)
        applicationId = "com.runnerschronicle.app"
        // Health Connect SDK 요구사항: minSdk 26 이상
        minSdk = 26
        targetSdk = 37
        // 스토어에 올릴 때마다 versionCode 를 1 씩 올린다
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProps.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // 개발: USB(adb reverse) 로 PC 서버. 설정 화면에서 와이파이 주소로 바꿀 수 있음
            buildConfigField("String", "DEFAULT_SERVER_URL", "\"http://127.0.0.1:8080\"")
        }
        release {
            buildConfigField("String", "DEFAULT_SERVER_URL", "\"$releaseServerUrl\"")
            signingConfigs.findByName("release")?.let { signingConfig = it }
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.health.connect.client)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}