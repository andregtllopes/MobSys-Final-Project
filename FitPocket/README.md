# FitPocket

> **Nota:** este projeto já foi completado com o Gradle (build.gradle.kts, settings.gradle.kts,
> tema `Theme.FitPocket`, ícone e KSP/Room configurados) e é compilado automaticamente pelo
> GitHub Actions (`.github/workflows/android-build.yml`, job `build-fitpocket`). As instruções
> originais abaixo eram para montagem manual num projeto novo do Android Studio; não são mais
> necessárias, mas ficam de referência.

## Como montar manualmente no Android Studio (instruções originais, opcional)

1. New Project > Empty Views Activity. Name: FitPocket, package: com.example.fitpocket,
   Language: Kotlin, Minimum SDK: API 34.
2. Apague a MainActivity e o activity_main.xml gerados.
3. Copie as pastas de `app/src/main/java/com/example/fitpocket` e `app/src/main/res/layout`
   deste zip para o mesmo lugar no seu projeto.
4. Substitua o AndroidManifest.xml pelo deste zip (mantenha o tema `Theme.FitPocket` que o
   Android Studio gerou; ele precisa ser um tema Material para o Snackbar funcionar).
5. Aplique as alterações do GRADLE_SNIPPETS.md e faça Sync.
6. Rode. No emulador use o botão "+1 (modo teste)"; no celular de verdade teste os sensores.
