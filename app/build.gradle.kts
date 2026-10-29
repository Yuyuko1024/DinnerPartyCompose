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

        // 只发布 arm64-v8a
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
        // 生成 BuildConfig
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    // MD3 outline 图标集（Icons.Outlined.*）
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    // 本地设置持久化
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // collectAsStateWithLifecycle / viewModel()
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.salt.ui)
    // 浮层玻璃：haze 采集与折射
    implementation(libs.haze.core)
    implementation(libs.haze.blur)
    implementation(libs.haze.glass)
    // 网络：Retrofit + OkHttp + Gson
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.gson)
    // 头像加载
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    // 头像裁剪
    implementation(libs.crop.kit)
    // 高德地图 3D，直接引用 app/libs 下的本地 AAR
    implementation(files("libs/AMap3DMap_11.3.100_AMapSearch_9.8.1_AMapLocation_11.3.101_20260924.aar"))
    // 自定义分段按钮
    implementation(libs.segmented.control)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}