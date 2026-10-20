# NERF Music (Android) — Prova de Conceito (PoC)

Aplicativo Android nativo de música desenvolvido em **Kotlin**, **Jetpack Compose** e **AndroidX Media3 (ExoPlayer)** com interface inteiramente em **Português do Brasil (PT-BR)**.

---

## 1. Arquitetura da Solução

O projeto segue a arquitetura recomendada do Android (MVVM com Separação Estrita de Responsabilidades):

```
app/src/main/java/com/nerf/music/
├── data/
│   ├── model/
│   │   ├── Track.kt                 # Metadados de catálogo (Título, Artista, Capa, Duração)
│   │   └── PlaybackState.kt         # Estado reativo da reprodução (Posição, Buffer, IsPlaying)
│   ├── network/
│   │   └── InnerTubeCatalogClient.kt# Cliente HTTP anônimo do catálogo InnerTube (WEB_REMIX pt-BR)
│   └── repository/
│       ├── MusicRepository.kt       # Interface limpa de acesso ao catálogo
│       └── MusicRepositoryImpl.kt   # Implementação do repositório
├── player/
│   ├── AudioPlayerService.kt        # MediaSessionService Media3 com suporte a segundo plano e notificações
│   ├── StreamResolver.kt            # Interface para resolução de streaming (desacoplada)
│   └── DemoStreamResolver.kt        # Fonte de áudio autorizada para validação do ExoPlayer (CC-BY)
└── ui/
    ├── MainActivity.kt              # Entrypoint com enableEdgeToEdge
    ├── theme/                       # Cores e tema Material 3 Dark
    ├── viewmodel/
    │   └── MusicViewModel.kt        # Conexão entre Repositório, MediaController e Compose
    └── screens/
        └── SearchScreen.kt          # Tela de busca com MiniPlayer persistente
```

---

## 2. Separação Estrita: Catálogo vs. Reprodução de Áudio

- **Catálogo de Metadados:** Obtido através de chamadas anônimas e públicas ao endpoint `https://music.youtube.com/youtubei/v1/search` com identificador `WEB_REMIX` e localização `pt-BR / BR`. Não requer login, conta de usuário ou chaves privadas.
- **Motor de Áudio:** Implementado via **AndroidX Media3 ExoPlayer** encapsulado em um `MediaSessionService` (`AudioPlayerService`). Garante notificação no sistema, foco de áudio e background playback.
- **Resolução de Stream:** Desacoplada sob a interface `StreamResolver`. Para validação de playback, utiliza fonte licenciada da suíte oficial do ExoPlayer (`https://storage.googleapis.com/exoplayer-test-media-0/play.mp3`), sem quebrar controles de acesso ou DRM.

---

## 3. Instruções de Compilação e Instalação

### Pré-requisitos instalados:
- **JDK:** OpenJDK 17 LTS
- **Android SDK:** `android-34` com `build-tools 34.0.0`
- **Gradle:** Wrapper 8.10.2 incluso no projeto

### Comandos:

1. **Executar os testes unitários e de integração:**
   ```bash
   .\gradlew.bat test
   ```

2. **Compilar o APK de desenvolvimento (Debug):**
   ```bash
   .\gradlew.bat assembleDebug
   ```
   *O APK gerado estará em:* `app\build\outputs\apk\debug\app-debug.apk`

3. **Instalar em dispositivo ou emulador conectado via ADB:**
   ```bash
   .\gradlew.bat installDebug
   # ou diretamente via adb:
   adb install -r app\build\outputs\apk\debug\app-debug.apk
   ```

---

## 4. Resultados dos Testes

- `InnerTubeCatalogClientTest.testEmptyQueryReturnsEmptyList`: **APROVADO** (0.28s)
- `InnerTubeCatalogClientTest.testRealCatalogSearchReturnsValidTracks`: **APROVADO** (0.85s)
- Compilação dos 45 módulos Gradle e empacotamento do APK: **APROVADO** (`BUILD SUCCESSFUL in 1m 21s`)
