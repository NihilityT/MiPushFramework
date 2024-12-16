plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("io.github.wurensen.android-aspectjx")
}

val mipushLib = file("libs/miuipushsdkshared_3_7_9.jar")
extra["mipushLib"] = mipushLib

android {
    namespace = "com.nihility.mipush_hook"
    compileSdk = 33

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }

    aspectjx {
        // 移除kotlin相关，编译错误和提升速度
        exclude("kotlin.jvm", "kotlin.internal")
        exclude("kotlinx.coroutines.internal", "kotlinx.coroutines.android")
        ajcArgs("-inpath", mipushLib.path)
        debug = true
    }
}

dependencies {
    compileOnly(files(mipushLib))

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}