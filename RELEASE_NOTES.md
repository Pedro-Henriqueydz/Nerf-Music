# ⚡ NERF Music v1.2.1 — Smart Downloads Quota Fulfillment & Real-Time Progress Notifications

Esta versão traz correções essenciais na arquitetura dos **Downloads Inteligentes (Smart Downloads)** e introduz um novo sistema de **feedback visual em tempo real através da barra de notificações do Android**.

---

### 🐛 1. Expansão Online de Candidatos para Quota Completa (`SmartDownloadManager`)
- **Problema corrigido:** Em instalações onde o usuário possuía um histórico curto ou poucas faixas favoritadas (ex: 23 músicas), a cota configurada (50, 100, 250 ou 500) não era preenchida por completo, gerando uma interrupção prematura da sincronização.
- **Resolução & Algoritmo de Sementes:**
  - Inspirado na engenharia reversa do APK oficial do YouTube Music (`FEoffline_mixtape` / `DefaultMusicAutoOfflineController`), o motor agora utiliza as faixas locais do usuário como **sementes de afinidade acústica**.
  - Quando os candidatos locais não cobrem a cota (`candidates.size < limit`), o gerenciador consulta a rota `/youtubei/v1/next` (`InnerTubeCatalogClient.getUpNextRadio`) de cada semente até completar o limite desejado.
  - Filtro integrado com `UserAffinityRepository` para descarte de faixas penalizadas (skip penalty/descurtidas).

---

### 📥 2. Notificação de Progresso em Tempo Real (`SmartDownloadNotificationHelper`)
- **Canal Dedicado:** `nerf_smart_downloads` com `NotificationManager.IMPORTANCE_LOW` para atualizações silenciosas sem vibrações ou alertas sonoros repetitivos a cada faixa.
- **Barra de Progresso Nativa:**
  - `NotificationCompat.CATEGORY_PROGRESS` com barra de progresso visual de `1 a X` (`setProgress(targetQuota, currentCount, false)`).
  - Atualização do título e texto dinâmico a cada download concluído: `(N de X) • [Título da Música]`.
- **Estados de Ciclo de Vida:**
  - **Em Andamento:** `setOngoing(true)` e `setOnlyAlertOnce(true)`.
  - **Concluído:** Notificação limpa com ícone `stat_sys_download_done` e resumo: *"X músicas salvas para reprodução offline"*.
  - **Pausado:** Notificação explicativa caso a conexão Wi-Fi seja perdida (`"Aguardando conexão Wi-Fi"`).

---

### 🔒 3. Concorrência e Estabilidade entre Processos
- **Mutex Estático Global (`globalSyncMutex`):**
  - Implementado no `SmartDownloadManager.Companion` para prevenir corridas de sincronização simultâneas entre disparos da UI (`MusicViewModel.triggerSmartDownloadSyncNow()`) e execuções agendadas do `WorkManager` (`SmartDownloadWorker`).
- **Instanciação Segura no `MusicViewModel`:**
  - Garantia de injeção singleton de repositórios em `initializePlayerController` independentemente da ordem de carregamento da biblioteca.

---

### 💎 4. Fidelidade e Áudio Original Preservados
- Todas as músicas complementadas via recomendações de catálogo mantêm a extração de áudio original no perfil **Lossless / Máxima Fidelidade** (Opus 48 kHz estéreo sem recompressão).
