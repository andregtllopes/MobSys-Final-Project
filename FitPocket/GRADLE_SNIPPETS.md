# Alterações no Gradle (ATENÇÃO: versões a conferir no seu Android Studio)

## 1) build.gradle.kts (raiz do projeto) — bloco plugins
```kotlin
plugins {
    // ... os que já existem ...
    id("com.google.devtools.ksp") version "<VERSÃO_KSP_COMPATÍVEL_COM_SEU_KOTLIN>" apply false
}
```
A versão do KSP tem o formato `<versão do Kotlin>-<versão do KSP>` e PRECISA casar com o Kotlin
do seu projeto (veja em Gradle/libs.versions.toml). Não confirmei a versão exata — confira em
https://github.com/google/ksp/releases

## 2) app/build.gradle.kts
```kotlin
plugins {
    // ... os que já existem ...
    id("com.google.devtools.ksp")
}

dependencies {
    // ... os que já existem (material, appcompat, core-ktx, lifecycle-runtime-ktx) ...
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    val room = "2.6.1"   // versão que conheço; pode haver mais nova, confira
    implementation("androidx.room:room-runtime:$room")
    implementation("androidx.room:room-ktx:$room")
    ksp("androidx.room:room-compiler:$room")
}
```
minSdk = 34, targetSdk/compileSdk = 34 (como nos exercícios).
