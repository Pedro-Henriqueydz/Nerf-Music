# 🎵 NERF Music v1.3.6 — Super Estabilidade, Relacionados Inteligentes e Sincronização Local

## 📱 Novidades desta versão (In-App)
- **Músicas Relacionadas sempre disponíveis:** A aba "Relacionados" agora sempre encontra recomendações e músicas semelhantes para qualquer faixa tocando, mesmo para músicas independentes ou underground.
- **Letras corrigidas:** Resolvido o problema onde músicas sem letra exibiam a palavra "Null".
- **Capas nos Downloads:** As artes de capa agora são salvas e embutidas diretamente dentro dos arquivos de música baixados.
- **Recuperação de Músicas no Celular:** Novo botão "Sincronizar Downloads" nas Configurações para recuperar e reconhecer automaticamente músicas salvas no aparelho que não apareciam no app.
- **Transições Suaves e Sem Cortes:** O crossfade e a troca de músicas agora acontecem de forma muito mais suave e sem interrupções bruscas.
- **Notificações mais estáveis:** Fim dos travamentos na barra de notificações e na tela de bloqueio durante downloads e reprodução em segundo plano.

---

## 🛠️ Notas Técnicas de Atualização (GitHub Release)

### v1.3.6 — Parser, Metadados, Relacionados & Sincronização
- **Arquitetura Multi-Tier de Recomendações (Fallback Quádruplo):**
  - **Tier 1 (InnerTube Browse):** Consulta inicial ao endpoint oficial do YouTube Music via `relatedBrowseId`.
  - **Tier 2 (UpNext Algorithmic Radio):** Se o endpoint oficial estiver vazio, consome a rádio algorítmica (`getUpNextRadio`) gerando a seção *"Músicas semelhantes"* com até 15 faixas recomendadas pelo algoritmo do YouTube.
  - **Tier 3 (Catálogo do Artista Principal):** Sanitização inteligente do nome do artista (`(part. ...)`, `feat.`, `ft.` e pontuações) para busca limpa gerando a seção *"Mais de <Artista>"*.
  - **Tier 4 (Busca Contextual):** Busca por título e artista sanitizados caso todas as anteriores falhem, garantindo que a aba nunca fique vazia.
  - **Auto-Sync em Transições:** Gatilho automático em `onMediaItemTransition`, avanço de faixas e abertura da folha de relacionados, além de botão manual de re-busca.
- **Sanitização Estrita de Letras:** Tratamento de strings literais (`"null"`, `"Null"`, `"Letra não disponível"`, `"Lyrics not available"`) em `InnerTubeCatalogClient.kt` e `LyricsRepository.kt`, garantindo renderização de estado vazio limpo na UI.
- **Embutimento de Tags e Artwork (`AudioMetadataTagger`):**
  - M4A/MP4: Injeção de caixas `covr` em átomos `moov/udta/meta/ilst` com recálculo e realocação de chunk offsets (`stco`/`co64`).
  - MP3: Tags ID3v2.3 completas (`TIT2`, `TPE1`, `TALB`, `APIC`).
  - Notificação e indexação no sistema via `MediaScannerConnection`.
- **Recuperação de Downloads Órfãos & MediaStore Sync:**
  - Varredura em `/sdcard/Music/`, `/sdcard/Music/NerfMusic/`, `/sdcard/Download/`, armazenamento interno e consulta ao provedor `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI`.
  - Repopulação automática no Room DB (`DownloadedTrackEntity`) com extração de tags e capas via `AudioMetadataTagger.extractMetadata`.
  - Botão *"Sincronizar Downloads"* integrado à tela de Configurações e seção de Armazenamento com feedback visual ao usuário.
- **Proteção contra Desinstalação Acidental:** Flag `android:hasFragileUserData="true"` no `AndroidManifest.xml` para preservação dos dados e mídias pelo sistema operacional Android.

### v1.3.5 — Core de Áudio, Crossfade Dual-Slot & Qualidade Dinâmica
- **Eliminação de Crashes no SystemUI:** Substituição de URIs `file://` locais por injeção direta de bytes via `MediaMetadata.setArtworkData(...)`, eliminando violações de sandbox e `SecurityException` no painel de mídia e tela de bloqueio do Android.
- **Crossfade com Arquitetura Dual-Slot Imutável:** Eliminação de colisões de player através de dois slots fixos permanentes (`playerA` e `playerB`). Isolamento de AudioFocus durante a rampa equal-power (`cos`/`sin`) e debounce de 500ms pós-seek para evitar cortes abruptos (*hardskips*).
- **Aplicação Fidedigna de Níveis de Qualidade de Áudio:** Resolução dinâmica de bitrate e codec de acordo com a rede ativa (Wi-Fi vs Dados Móveis) e modo Economia de Dados, com invalidação em tempo real do cache de stream (`clearStreamCache()`).
- **Validação Estrita de Integridade de Mídia Baixada:** Verificação de tamanho mínimo (> 1 KB) em todos os codecs (`opus`, `m4a`, `webm`, `mp4`, `mp3`, `ogg`, `aac`), prevenindo paradas durante o streaming local.
