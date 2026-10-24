# ⚡ NERF Music v1.2.0 — Smart Downloads & Lossless Audio Quality

Esta versão introduz a arquitetura de **Smart Downloads (Downloads Inteligentes)** com paridade total à experiência do YouTube Music, somada ao suporte nativo de **Qualidade de Mídia dos Downloads com padrão Lossless / Máxima Fidelidade**.

---

### ⚡ 1. Arquitetura de Smart Downloads (`SmartDownloadManager` & `SmartDownloadWorker`)
- **Orquestração em Segundo Plano via Android WorkManager:**
  - `SmartDownloadWorker` (`CoroutineWorker`) agendado com políticas periódicas de 24 horas (`ExistingPeriodicWorkPolicy.UPDATE`) e disparos imediatos sob demanda via `OneTimeWorkRequestBuilder`.
  - Restrições estritas de ambiente configuráveis:
    - Rede não tarifada (`NetworkType.UNMETERED`) para sincronização exclusiva em Wi-Fi (configuração padrão ativada).
    - Preservação energética do dispositivo com `setRequiresBatteryNotLow(true)`.
- **Heurística de Síntese Ponderada de Candidatos:**
  - **40% Familiaridade Imediata:** Músicas favoritadas (`likedTracksFlow`) e histórico recente de alta afinidade (`userHistoryRepository.getRecentlyPlayed()`).
  - **30% Coleções do Usuário:** Músicas presentes nas playlists criadas pelo usuário (`playlistsFlow`).
  - **30% Descoberta & Afinidade Acústica:** Recomendações e faixas correlacionadas pelo `SmartRadioEngine`.
- **Política de Rotação Automática de Quotas:**
  - Cotas selecionáveis: **50, 100, 250 ou 500 músicas**.
  - Separação de domínio em `DownloadedTrack` com flag `isSmartDownload: Boolean`. Músicas baixadas explicitamente pelo usuário são marcadas como permanentes e **nunca** sofrem rotação ou expurgo automático.
  - Ao atingir a cota ou quando novas faixas entram em alta afinidade, o motor remove automaticamente as faixas inteligentes mais antigas e menos reproduzidas.

---

### 💎 2. Pipeline de Áudio & Qualidade Lossless Nativa
- **Resolução Sem Recompressão (`NewPipeStreamResolver`):**
  - Implementação da sobrecarga `resolveStreamUrl(videoId: String, targetQuality: AudioQuality?)`.
  - Modo **Lossless (Padrão de Fábrica)**: Seleciona o fluxo de áudio nativo de mais alta taxa de amostragem e bitrate entregue pelo catálogo (Opus a 48 kHz até 256 kbps VBR e AAC/M4A), sem transcodificação ou degradação acústica.
- **Configuração Hierárquica de Fidelidade (`DownloadAudioQuality`):**
  - *Lossless (Máxima Fidelidade)* — Padrão.
  - *Muito Alta (~320 kbps equivalente)*.
  - *Alta (256 kbps)*.
  - *Normal (128 kbps)* — Para máxima economia de espaço.

---

### 🔄 3. Sincronização Reativa de Estado (`LibraryRepositoryImpl`)
- **StateFlows Compartilhados & Listener de SharedPreferences:**
  - Compartilhamento singleton dos fluxos reativos (`sharedDownloadedTracksFlow`, `sharedLikedTracksFlow`, `sharedPlaylistsFlow`).
  - Registro de `SharedPreferences.OnSharedPreferenceChangeListener`: Garante que downloads efetuados pelo `WorkManager` em segundo plano atualizem instantaneamente a UI em tempo real, sem necessidade de reinicialização do app ou polling.

---

### 🎨 4. Interface do Usuário (Compose)
- **Subtela de Armazenamento (`SettingsSubScreen.STORAGE`):**
  - Seção dedicada de Smart Downloads com switch mestre de ativação.
  - Seletor de chips de cota (50, 100, 250, 500 músicas) com cálculo dinâmico de espaço em disco estimado (*~519 MB em padrão Lossless para 100 faixas*).
  - Switch de download restrito a Wi-Fi.
  - Card de status em tempo real com indicador de progresso e botão de sincronização manual imediata.
- **Subtela de Qualidade da Mídia (`SettingsSubScreen.MEDIA_QUALITY`):**
  - Nova seção dedicada para qualidade de áudio dos downloads com descrição técnica e seleção em rádio.
- **Biblioteca (`LibraryScreen`):**
  - Visualização imediata do quantitativo no cartão e chip "Músicas Baixadas" (`Mix Offline`).
