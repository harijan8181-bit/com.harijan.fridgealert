import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.harijan.fridgealert"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.harijan.fridgealert"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 식품안전나라 API 키는 git에 올라가지 않는 local.properties 에 FOOD_API_KEY=... 로 넣는다
        val localProps = Properties().apply {
            rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
        }
        buildConfigField("String", "FOOD_API_KEY", "\"${localProps.getProperty("FOOD_API_KEY", "")}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    // DB 마이그레이션 테스트가 app/schemas 의 구조 기록을 읽을 수 있게
    sourceSets.getByName("androidTest") {
        assets.srcDir("$projectDir/schemas")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// Room DB 구조 기록 (app/schemas/*.json, 버전 관리에 포함할 것)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    // Jetpack Compose & Material 3
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation(libs.androidx.activity.compose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Google AdMob
    implementation("com.google.android.gms:play-services-ads:23.3.0")

    // ML Kit 온디바이스 한국어 OCR (오프라인/비용 0원)
    implementation("com.google.mlkit:text-recognition-korean:16.0.1")

    // 바코드 스캐너 (구글 Play 서비스 제공 화면 — 카메라 권한 불필요)
    implementation("com.google.android.gms:play-services-code-scanner:16.1.0")

    // Room DB
    val roomVersion = "2.8.5"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    // room-testing이 요구하는 직렬화 버전으로 맞춤 (앱은 1.7.3이 잡혀 마이그레이션 테스트가 실행 중 깨졌음)
    implementation(platform("org.jetbrains.kotlinx:kotlinx-serialization-bom:1.8.1"))

    // WorkManager (백그라운드 스케줄러)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation("org.json:json:20240303") // 단위 테스트에서 실제 JSON 파서 사용
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation("androidx.room:room-testing:$roomVersion")
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
