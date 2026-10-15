plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "cc.dinnerparty.compose"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "cc.dinnerparty.compose"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // 高德 3D 地图 AAR 里 arm64-v8a / armeabi-v7a 各带一套 .so，本项目只发布 arm64。
        ndk {
            abiFilters += "arm64-v8a"
        }
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        // 网络日志只在 debug 打开，靠 BuildConfig.DEBUG 判断
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    // MD3 outline 图标集（Icons.Outlined.*）。版本由上面的 Compose BOM 统一管理。
    // 该库已停止更新、被 BOM 固定在 1.7.8，属正常现象。
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    // 本地设置持久化。取代 SharedPreferences：读写走 Flow / 挂起函数，不需要自己切线程。
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // collectAsStateWithLifecycle / viewModel()
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.salt.ui)
    // 网络：Retrofit + OkHttp + Gson。后端全部是 JSON，用 Gson 转换器即可。
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
    // 高德地图 3D（Map 11.3.100 + Search 9.8.1 + Location 11.3.101）。
    // 官方只以 AAR 分发、不上 maven，所以直接引用 app/libs 下的本地文件。
    implementation(files("libs/AMap3DMap_11.3.100_AMapSearch_9.8.1_AMapLocation_11.3.101_20260924.aar"))
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}