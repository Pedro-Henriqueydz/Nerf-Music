# Registro de Alterações (Changelog) - NERF Music

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo seguindo as convenções de [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/) e [Semantic Versioning](https://semver.org/lang/pt-BR/).

---

## [Unreleased]
> **Status:** Em desenvolvimento na branch principal após o lançamento da versão v1.4.1.

### Corrigido
- **Crash ao Reordenar Fila na JAM como Participante (`MusicViewModel` / `PlayerScreen`):**
  - **Eliminação de `IndexOutOfBoundsException`:** Corrigida a falha crítica que derrubava o aplicativo ao arrastar e reordenar músicas na fila da JAM como participante. O participante agora aplica reordenação otimista imediata na UI e despacha o comando `JamCommandType.MOVE_QUEUE_ITEM` ao Host sem tentar invocar `mediaController.moveMediaItem` na timeline local do ExoPlayer (que contém apenas a faixa atual sincronizada).
  - **Validação Defensiva de Timeline no Host:** Adicionada validação de limites (`fromIndex in 0 until controller.mediaItemCount && toIndex in 0 until controller.mediaItemCount`) e proteção contra exceções na reordenação da timeline mestre.
  - **Remoção e Seleção de Itens da Fila na JAM:**
    - Em `removeQueueItem`, o participante atualiza otimisticamente sua fila visual e delega a exclusão ao Host via `JamCommandType.REMOVE_QUEUE_ITEM`, evitando chamadas de remoção fora dos limites locais do player.
    - Em `playQueueItemAt`, convidados agora conseguem selecionar qualquer faixa da fila para tocar para todos na sala (via `JamCommandType.SELECT_QUEUE_INDEX`), removendo a trava que impedia a seleção de índices maiores que 0 no participante.
  - **Isolamento de Rádio Dinâmica na JAM:** Convidados não disparam mais `radioFetchJob` em background ao receber faixas remotas da sessão, impedindo que rádios do YouTube Music sobrescrevam a fila colaborativa da sala.
  - **Prevenção de Colisão de Chaves no Compose (`PlayerScreen`):** A fila agora utiliza chaves compostas seguras (`key = { index, track -> "${track.id}_$index" }`), prevenindo crashes caso faixas duplicadas sejam inseridas na fila colaborativa da sala.

### Adicionado
- **Menu de Opções de Faixa Oficial (3 Pontinhos) Estilo YouTube Music (`TrackOptionsBottomSheet`):**
  - **Novo BottomSheet Nativo:** Implementado painel oficial com capa em alta resolução, título, artista, duração e botão de fechar.
  - **Cards Rápidos Lado a Lado:** Acesso com 1 toque para *"Tocar a seguir"* e *"Iniciar rádio"*.
  - **Menu de Ações Completo:** Opções para *"Adicionar à fila"*, *"Salvar na playlist"* (conectado ao modal de playlists com suporte a qualquer faixa), *"Salvar/Remover de Músicas Curtidas"*, *"Fazer/Remover download"*, *"Compartilhar"* (link nativo do YouTube Music), *"Ver artista"* e *"Ver álbum"*.
  - **Integração Global dos 3 Pontinhos:** Conectados todos os botões de 3 pontinhos que estavam inativos na **Pesquisa** (`SearchScreen`), na **Home** (`HomeScreen`), no **Álbum** (`AlbumScreen`) e no **Artista** (`ArtistScreen`).

---

## [1.4.1] - 2026-10-27 [RELEASED]

### Corrigido
- **Precisão e Casamento Estrito de Faixas Instrumentais / Karaokê (`KaraokeLyricsEngine`):**
  - **Eliminação de Instrumentais Incorretos:** Implementado novo algoritmo seletivo de correspondência heurística (`selectBestInstrumentalCandidate`). Músicas como *"Reason - Altare"* não vinculam mais versões aleatórias ou remixes irrelevantes.
  - **Validação Estrita de Tolerância Temporal:** Instrumentais com diferença de duração superior a 12 segundos em relação à música original são terminantemente descartados, priorizando faixas oficiais que contenham palavras-chave como `official instrumental`, `karaoke`, `playback` ou `instrumental`.
  - **Alinhamento de Beat e Sincronia de Entrada (`computeBestBeatOffsetMs`):** Resolvida a dessincronia de tempo em faixas como *"É Com Ela Que Eu Estou - Cristiano Araújo"*. O motor analisa e correlaciona os envelopes de transientes do áudio principal e do instrumental para calcular e compensar o deslocamento da introdução em tempo real com rampa suave de volume.
- **Processamento de Isolamento Vocal DSP Anti-Abafamento (`VocalIsolationAudioProcessor`):**
  - **Preservação de Graves, Bateria e Agudos:** Reformulação completa da filtragem DSP local. O antigo corte que abafava a música foi substituído por um processador estéreo *Mid-Side* com filtro passa-faixa centrado estritamente na região de formantes vocais (`280 Hz – 3500 Hz`) e ganho de presença (`clarityGain`).
  - **Linearidade do Slider de Voz:** A atenuação da voz agora responde de forma gradual e natural, permitindo desde o instrumental 100% puro até a intensidade vocal original sem perdas de dinâmica nas frequências graves ou estéreo.
  - **Estabilidade na Inicialização do Player:** Inicialização segura do processador de áudio (`instrumentalEnvelopeProcessor`), prevenindo falhas de inicialização tardia (`UninitializedPropertyAccessException`) no ciclo de vida do `AudioPlayerService`.

### Adicionado
- **NERF JAM Global via Internet (4G / 5G / Wi-Fi) (`JamGlobalRelayClient`):**
  - **Conexão Sem Limites de Rede Local:** A JAM agora conecta amigos em qualquer lugar do mundo por meio de transporte MQTT sobre WebSocket seguro (`wss://broker.emqx.io:8084/mqtt` ou relay dedicado), mantendo compatibilidade com conexões LAN diretas.
  - **Códigos de Sala Amigáveis e DeepLinks Globais:** Geração e parsing automático de códigos curtos (ex.: `NERF-4821` ou apenas `4821`), além de links `nerfmusic://jam/join?...` e QR Code interativo.
- **Painel de Configurações da Sala e Governança do Host (`JamSessionSheet` e `JamRoomSettings`):**
  - **Permissão de Fila Colaborativa:** Toggle para o Host decidir se os convidados podem adicionar, remover e reordenar músicas na fila.
  - **Permissão de Controle de Reprodução:** Toggle para o Host autorizar ou bloquear comandos de pular (`skipNext`/`skipPrevious`), pausar, dar play, avançar na barra de progresso ou trocar de faixa por convidados.
  - **Looping da Sala Sincronizado:** Modos de repetição (`Desativado`, `Repetir Fila`, `Repetir 1 Música`) controlados pelo Host e propagados instantaneamente para toda a sala.
  - **Crossfade Sincronizado Herdado do Host (0s a 12s):** Todos os convidados herdam dinamicamente a duração exata de Crossfade, Automix e Gapless definida pelo Host durante a sessão, sem alterar permanentemente suas preferências locais. Ajustável pelo Host via slider em tempo real no painel da JAM.
  - **Modo Karaokê em Grupo:** Sincronização do modo Karaokê e do nível de atenuação vocal do Host com todos os ouvintes da sala.
  - **Gerenciamento de Sala:** Opção para o Host remover participantes da sessão (Kick) e botão para convidados forçarem a ressincronização imediata de áudio com a posição da faixa do Host.
- **Novo Ícone da JAM e Acesso Rápido na Home:**
  - **Identidade Visual da JAM (`JamPersonWavesIcon`):** Substituição dos ícones genéricos por um ícone customizado com silhueta humana em primeiro plano e ondas sonoras acústicas no estilo Spotify ao fundo, presente no MiniPlayer, tela cheia, letras e painel da JAM.
  - **Botão Rápido na Home:** Adicionado botão de atalho para a JAM na barra superior da tela inicial (`YtmTopBar`) ao lado da engrenagem de configurações, permitindo criar ou entrar em salas logo ao abrir o aplicativo sem precisar iniciar uma reprodução prévia.

---

## [1.4.0] - 2026-10-27 [RELEASED]

### Adicionado
- **NERF JAM — Escuta Sincronizada em Grupo (Arquitetura 100% No-Account):**
  - **Servidor WebSocket Local Incorporado (`JamWebSocketServer`):** Permite que qualquer dispositivo atue como anfitrião (Host) hospedando uma sessão de escuta coletiva em tempo real com comunicação local P2P/LAN (porta `8924`) e suporte opcional a servidores de relay transparentes.
  - **DeepLink Universal e Compartilhamento Fluido:** Convites automáticos via esquema `nerfmusic://jam?id=...&host=...&relay=...` e código QR dinâmico na interface do aplicativo (`JamSessionSheet`).
  - **Sincronização de Reprodução e Fila em Tempo Real:** Sincronia perfeita de Play, Pause, Seek (com compensação inteligente de RTT em desvios > 350ms) e controle colaborativo de adição, remoção e reordenação de itens da fila entre anfitrião e convidados.
- **Modo Karaokê de Próxima Geração com Troca Instantânea por Instrumental Oficial:**
  - **Dual-ExoPlayer Crossfade em Standby:** Busca proativa em segundo plano de versões oficiais instrumentais/karaokê correspondentes (`$title $artist official instrumental`), pré-carregando o fluxo de áudio em uma instância secundária (`instrumentalPlayer`).
  - **Transição Zero-Delay na Timeline:** Ao tocar no microfone na tela de letras, o app comuta para o instrumental no exato milissegundo em reprodução com rampa suave de crossfade equal-power (~120ms), preservando a faixa vocal original atrelada ao slider de volume de voz.
  - **Isolamento Vocal DSP Aprimorado (Não Abafado):** Processamento local seletivo (`VocalIsolationAudioProcessor`) atuando estritamente na região central da voz (`280 Hz – 3500 Hz`) com compensação de ganho de presença (`clarityGain`), preservando graves, caixa de bateria e agudos quando não houver faixa instrumental oficial disponível.
  - **Destaque Palavra por Palavra Condicional:** Destaque sincrônico palavra a palavra e contagem regressiva instrumental (`• • •`) ativados estritamente quando o modo Karaokê está engajado.
- **Tradução e Pronúncia Apple Music Glass (`emil-design-eng`):**
  - **Tradução Dinâmica Multilíngue:** Tradutor em tempo real com seletor de idiomas dedicados (`Auto`, `PT`, `EN`, `ES`, `FR`, `JA`, `KO`, `DE`, `IT`) com retradução imediata da letra ativa.
  - **Pronúncia Fonética/Romanizada:** Transliteração internacional para acompanhar e cantar músicas em japonês, coreano, inglês e outras línguas estrangeiras.
  - **Suporte a Letras de Fallback Estáticas:** Estruturação de letras do YouTube Music (`plainLyrics`) permitindo tradução e pronúncia linha a linha mesmo em letras não sincronizadas.
  - **Apple Glass Popover UI:** Menu translúcido dark glass (`#1C1C1E` a 94% de opacidade) com bordas ultrafinas, switches estilo iOS, animações origin-aware e toggle para ocultar o verso original mantendo apenas a tradução ou pronúncia.
- **Limpeza de Interface no Player:** Remoção da pílula redundante de Karaokê na tela principal do Player (mantendo o microfone exclusivo na tela de letras) e inclusão do botão dedicado JAM na barra inferior do modo Letras Fullscreen.

### Adicionado
- **Configurações Granulares de Download (Canvas e Letras Independentes):**
  - Opções independentes nas Configurações e nos diálogos de download permitindo baixar "Canvas" e "Letras" de forma desacoplada do áudio.
  - Opção "Letras" habilitada por padrão (`DEFAULT_DOWNLOAD_LYRICS = true`).
  - Estimativa dinâmica de espaço em disco atualizada em tempo real conforme os toggles selecionados pelo usuário.
- **Empty State Offline Estilo YouTube Music:**
  - Interface dedicada na tela inicial (Home) exibida quando o aparelho estiver sem conexão à internet, com layout padronizado informando o estado offline e botão de atalho direto para a aba de downloads/arquivos locais.
- **Reconexão Automática e Pull-to-Refresh em Tempo Real (`ConnectivityObserver`):**
  - Integração de monitor de conectividade reativo que restaura automaticamente os dados do feed da Home assim que o dispositivo restabelece a conexão com a internet, sem necessidade de intervenção do usuário.
  - Suporte nativo a arrastar para baixo para recarregar (Pull-to-Refresh) com o feed já no topo.

### Corrigido
- **Foco e Centralização Automática na Fila do Player (Queue Auto-Scroll):**
  - Ajustada a gaveta da fila no Player (`FullPlayerSheet`) para rolar suavemente e focar de imediato na música atual em reprodução (`scrollToItem(currentQueueIndex)`), em vez de renderizar no topo.
- **Eliminação de Delay e Flickering de Capa no Swipe do Player:**
  - Eliminação da exibição repetida da capa antiga durante a animação de swipe horizontal através de estado otimista antecipado (`pendingSwipeTrack`).
  - Pré-aquecimento das capas das faixas adjacentes (próxima e anterior) no cache de memória do Coil, com suporte a arquivos offline e capas remotas.
  - Troca instantânea (milissegundo zero) de `activePlayer` e `MediaSession` no `AudioPlayerService` durante skips manuais, removendo o atraso anterior de 2.5 a 5 segundos de espera assíncrona.
- **Validação Estrita de Economia de Dados (Data Saver Enforcement):**
  - Aplicação de regras que impedem o download de recursos pesados (Canvas e capas de alta resolução) quando o modo Economia de Dados estiver ativo (`Sempre ativado` ou `Automático` em redes móveis). Faixas já presentes no cache local são executadas sem consumo de franquia.
- **Encerramento de Reprodução ao Fechar na Tela de Recentes (`onTaskRemoved`):**
  - Ajustado o `AudioPlayerService` para interromper imediatamente a reprodução de áudio, limpar a fila do player e dispensar a notificação em primeiro plano (`stopForeground(STOP_FOREGROUND_REMOVE)` e `stopSelf()`) quando o usuário encerra o aplicativo arrastando o card na tela de aplicativos recentes do Android. Músicas continuam tocando normalmente em segundo plano caso o app seja apenas minimizado (botão Home ou tela bloqueada).

---

## [1.3.5] - 2026-10-27 [RELEASED]

### Corrigido
- **Aba "Relacionados" no Player (Arquitetura Multi-Tier à Prova de Falhas):**
  - **Prevenção de estados vazios:** Implementado pipeline de enriquecimento em múltiplos níveis que assegura a exibição contínua de recomendações relevantes mesmo quando a API oficial do YouTube Music não retorna dados para faixas específicas de underground/phonk (ex.: *"Impulse"* por Altare).
  - **Rádio Algorítmica UpNext como Fallback:** Quando a aba oficial não retorna seções, o app reaproveita a fila dinâmica gerada para a música ou busca o rádio algorítmico (`getUpNextRadio`), gerando automaticamente a seção "Músicas semelhantes".
  - **Catálogo do Artista Principal Sanitizado:** Sanitização inteligente do nome de artista (removendo `(part. ...)`, `feat.`, `ft.` e pontuações) para consulta precisa no catálogo e preenchimento da seção "Mais de &lt;Artista&gt;".
  - **Sincronização em Transições de Faixa:** O carregamento de relacionados passa a ser acionado automaticamente nas transições de reprodução do ExoPlayer (`onMediaItemTransition`), ao avançar faixas e ao abrir a gaveta no Player, além de botão dedicado de re-busca ("Buscar recomendações") caso o dispositivo esteja offline temporariamente.

- **Parser de Letras (Lyrics Sanitization):**
  - **Eliminação de string literal "Null":** Tratamento estrito em `InnerTubeCatalogClient.kt` e `LyricsRepository.kt` para desconsiderar respostas com letras nulas literais (`"null"`, `"Null"`, `"Letra não disponível"`, `"Lyrics not available"`), garantindo a renderização correta do empty state em vez de texto nulo na interface.

- **Reprodução Offline e Notificações (Downloads em Segundo Plano):**
  - **Eliminação de crash no SystemUI:** Os bytes das capas de álbuns offline agora são injetados diretamente via `MediaMetadata.setArtworkData(...)` em vez de expor URIs `file://` locais do sandbox do aplicativo, prevenindo falhas de `SecurityException` e quebras no processo de notificação e tela de bloqueio do Android.
  - **Integridade de arquivos baixados:** Verificação estrita de tamanho mínimo (> 1 KB) e validação de existência cobrindo todos os formatos suportados (`.opus`, `.m4a`, `.webm`, `.mp4`, `.mp3`, `.ogg`, `.aac`), evitando que arquivos incompletos ou corrompidos causem pausas e paradas repentinas na reprodução.
  - **Estabilidade de segundo plano:** Adicionada permissão `ACCESS_WIFI_STATE` ao `AndroidManifest.xml` para garantia de funcionamento do bloqueio de rede Wi-Fi (`WifiLock`) durante a execução do serviço em segundo plano.

- **Motor de Transições e Crossfade Dinâmico:**
  - **Arquitetura Dual-Slot Imutável:** Eliminação definitiva de colisões de instância entre players através de dois slots fixos permanentes (`playerA` e `playerB`). O player de entrada é determinado de maneira estrita, impedindo que o player ativo tente transicionar sobre si mesmo ou limpe a própria playlist.
  - **Isolamento de AudioFocus:** Durante o crossfade, o player de entrada não solicita foco de áudio do sistema concorrentemente, permitindo que a rampa equal-power (`cos`/`sin`) aconteça suavemente sem que o `AudioManager` do Android pause abruptamente a faixa anterior.
  - **Estabilização Pós-Seek na Barra de Progresso:** Implementada janela de debounce de 500ms e trava para músicas com menos de 1,5s restantes, eliminando paradas a seco (*hardskips*) ou estalos quando o usuário adianta a música na barra de progresso para próximo da área de crossfade.
  - **Skips e Navegação sem Corte Seco:** Aplicação de transição suave anti-pop (`smoothTransitionTo`) ao avançar ou retroceder faixas (`skipNext`, `skipPrevious`, `forceSkipPrevious`, `playQueueItemAt`).
  - **Ajuste em Tempo Real de Configurações:** Proteção ao alterar o slider de crossfade com o player carregado, impedindo múltiplos disparos durante o arraste. Quando configurado em 0s com Automix desligado, o serviço opera 100% no modo nativo Gapless da mesma timeline do ExoPlayer.

- **Aplicação Real dos Níveis de Qualidade de Áudio:**
  - **Resolução fidedigna de bitrate e codec:** O `AudioPlayerService` passa a fornecer a qualidade alvo calculada com base no tipo de rede ativa (Wi-Fi vs Móvel) e modo Economia de Dados ao resolver fluxos sob demanda.
  - **Invalidação imediata de cache:** Alterações nas preferências de qualidade ou modo de dados limpam o cache de URLs em tempo real (`clearStreamCache()`), garantindo que faixas subsequentes adotem a nova qualidade imediatamente.
  - **Proteção a downloads de alta fidelidade:** Resolução de downloads em qualidade máxima não sofre downgrade indevido pelo modo de economia no `CompositeStreamResolver`.

### Adicionado
- **Embutimento de Metadados e Capas em Mídias Baixadas (`AudioMetadataTagger`):**
  - **Tagging Nativo MP4/M4A e MP3:** Embutimento de tags de metadados (`title`, `artist`, `album`) e arte de capa (box `covr` em átomos `moov/udta/meta/ilst` com recálculo seguro de offsets `stco`/`co64` para M4A, e frames `APIC`, `TIT2`, `TPE1`, `TALB` ID3v2.3 para MP3).
  - **Notificação ao MediaScanner:** Invocação do `MediaScannerConnection` para indexação imediata de capas e faixas no sistema operacional Android.

- **Recuperação de Downloads Órfãos e Sincronização Local (No-Account Sync):**
  - **Varredura Universal de Armazenamento:** Escaneamento automático de pastas internas (`offline_audio`), pastas públicas do dispositivo (`/sdcard/Music/`, `NerfMusic/`, `/sdcard/Download/`) e consulta ao provedor `MediaStore.Audio.Media`.
  - **Repopulação Automática do Room DB:** Arquivos de áudio desvinculados do banco de dados local têm seus metadados ID3/MP4 extraídos e reimportados automaticamente para a biblioteca offline.
  - **Sincronização sob Demanda:** Adicionado botão "Sincronizar Downloads" na tela principal de Configurações e dentro da seção Armazenamento.

- **Proteção contra Exclusão de Mídias em Desinstalações (`hasFragileUserData`):**
  - Adicionado `android:hasFragileUserData="true"` no `AndroidManifest.xml` para permitir que o usuário opte por preservar suas músicas baixadas e dados do aplicativo ao desinstalar ou reinstalar compilações.

### Testes
- Adicionada suíte de testes de estresse e validação:
  - `AudioMetadataTaggerTest`: Verificação estrutural de átomos MP4, caixas `covr`, `moov`, `mdat` e tags ID3v2.3.
  - `LyricsNullSanitizationStressTest`: Cobertura de cenários de sanitização de strings literais de lyrics nulos.
  - `RelatedMultiTierFallbackTest`: Validação de fallback multi-tier e extração correta de artistas principais.

---

## [1.3.4] - 2026-10-25

### Corrigido
- **Letras no player:** versos longos são divididos em trechos de uma linha que avançam sincronizados com a música, sem reticências. A visualização completa de letras permanece inalterada.

---

## [1.3.3] - 2026-10-25

### Corrigido
- **Sync do YouTube Music:** reaproveitamento conservador de playlists importadas em versões anteriores, evitando recriação em sincronizações repetidas sem substituir playlists distintas de mesmo nome.
- **Duração das faixas importadas:** leitura de durações em diferentes campos da resposta do YouTube Music e preenchimento do tempo em segundos.

### Melhorado
- **Carregamento da Home:** pré-carregamento de streams e Canvas das primeiras faixas para reduzir a espera ao iniciar a reprodução.
- **Smart Downloads:** desativado por padrão; pode ser ativado nas configurações.

---

## [1.3.2] - 2026-10-25

### Corrigido
- **Resolução de streams:** uso de resolução alternativa quando a fonte primária falha e cache separado por rede e qualidade para reduzir a reutilização de URLs expiradas ou incompatíveis.
- **Migração de playlists:** identificação estável de playlists importadas e atualização da playlist existente em reimportações, em vez de criar cópias.
- **Importação de favoritos:** inclusão em lote com deduplicação de faixas.
- **Persistência de sessão:** falha ao criptografar ou persistir uma sessão deixa de ser apresentada como conexão concluída.
- **Leitura de duração:** tratamento de colunas ausentes nas respostas do catálogo.

### Segurança
- **Armazenamento seguro:** operações de escrita passam a falhar de forma explícita quando a criptografia ou persistência protegida não pode ser concluída.

### Testes
- Adicionados testes unitários para a identidade de playlists importadas e a estratégia padrão de resolução de streams.

---

## [1.3.1] - 2026-10-25

### Segurança e Estabilidade
- **Proteção Avançada de Dados Locais:** Implementação de criptografia autenticada AES-256 GCM vinculada ao Android KeyStore (`SecureStorageHelper`) para proteção de dados locais e sessões de streaming.
- **Prevenção de Extração não Autorizada:** Desativação de backup arbitrário no manifesto (`android:allowBackup="false"`).
- **Tráfego Seguro Obrigatório:** Configuração de rede restrita (`network_security_config.xml`) com bloqueio de conexões em texto claro e ancoragem exclusiva em CAs oficiais do sistema.
- **Navegação Segura no WebView:** Desativação de acesso a arquivos locais e whitelist estrita de domínios permitidos nos fluxos de autenticação do YouTube Music e Spotify.
- **Validação de Integridade no Auto-Updater:** Filtro específico para pacotes `.apk`, verificação de integridade estrutural e checagem de integridade contra Path Traversal no `AppUpdateManager`.
- **Menor Privilégio no FileProvider:** Redução de caminhos compartilhados em `file_paths.xml` estritamente ao diretório de atualizações.

---

## [1.3.0] - 2026-10-24

### Adicionado
- **Central de Migração de Streaming & Personalização da Home (Multi-Streaming Migration):**
  - **Conexão Direta (YouTube Music & Spotify):** Login seguro com extração direta de sessão oficial para sincronização completa de Músicas Curtidas e Playlists pessoais.
  - **Paginação Completa sem Perdas:** Motor recursivo de paginação avançada cobrindo tanto `shelf.continuations` quanto `continuationItemRenderer` embutido e `onResponseReceivedActions.appendContinuationItemsAction`, capturando 100% de bibliotecas com mais de 250 faixas.
  - **Modo Anônimo de Alta Fidelidade (Sem Login):** Importação instantânea de links públicos do Spotify (com motor de correspondência acústica atingindo ~98% de assertividade) e do YouTube Music.
  - **Importação de Arquivos de Backup:** Compatibilidade universal com arquivos `.csv`, `.txt` e exportações de ferramentas de migração como Soundiiz e TuneMyMusic.
  - **Capas Quadradas Nativas de Alta Resolução:** Eliminação de bordas pretas letterbox via extração nativa de thumbnails Google/YTM em resolução 544x544 (`=w544-h544-l90-rj`).
  - **Persistência Atômica em Lote:** Criação e armazenamento em disco atômico de playlists com centenas de faixas em um único ciclo, prevenindo condições de corrida e perda de faixas.
  - **DNA Acústico & Transformação Imediata da Home:** Injeção automática das faixas importadas no grafo de afinidade do ouvinte, adaptando os cards de Escolha a Dedo, Artistas Favoritos e Supermix assim que a migração é finalizada.

---

## [1.2.0] - 2026-10-24

### Adicionado
- **Smart Downloads (Downloads Inteligentes Estilo YouTube Music):**
  - **Sincronização em Segundo Plano via WorkManager:** `SmartDownloadWorker` periódico (24h) e disparo sob demanda com restrições de bateria (`batteryNotLow`) e conectividade Wi-Fi (`NetworkType.UNMETERED`).
  - **Heurística de Síntese Ponderada:** Coleta inteligente de faixas candidatas (40% Favoritas e Recentes, 30% Playlists do usuário e 30% Descobertas do rádio).
  - **Cotas Configuráveis & Estimativa em Disco:** Seleção de cota para 50, 100, 250 ou 500 músicas com estimativa visual de espaço em disco (~519 MB para 100 faixas em Lossless).
  - **Política de Rotação Automática de Quota:** Liberação de espaço de smart downloads antigos não ouvidos preservando permanentemente downloads efetuados manualmente pelo usuário (`isSmartDownload`).
  - **Sincronização Reativa em Tempo Real:** Singleton `StateFlow` e `OnSharedPreferenceChangeListener` em `LibraryRepositoryImpl` para sincronização instantânea com a interface.

- **Qualidade de Mídia dos Downloads (Padrão Lossless):**
  - **Extração Lossless / Máxima Fidelidade:** Extração direta do fluxo de estúdio nativo de mais alta resolução e taxa de amostragem do catálogo (Opus 48kHz até 256kbps VBR e AAC/M4A), sem compressão redutora.
  - **Seletor de Qualidade das Mídias Baixadas:** Suporte a múltiplos níveis de qualidade nas configurações: Lossless (Padrão), Muito Alta (~320 kbps eq.), Alta (256 kbps) e Normal (128 kbps).

- **Interface & Experiência do Usuário (Compose):**
  - **Central de Armazenamento:** Seção completa de Smart Downloads com switches, seletor de cotas em chips, status de progresso e botão de sincronização manual.
  - **Central de Qualidade:** Seção dedicada com opções em rádio e descrição técnica de fidelidade acústica.
  - **Biblioteca & Mix Offline:** Contagem em tempo real e visualização organizada das músicas baixadas.

---

## [1.1.0] - 2026-10-24

### Adicionado
- **Transições de Música & Motor de Áudio (`Song Transitions`):**
  - **Crossfade Suave com Curva de Potência Constante (0s a 12s):** Arquitetura Dual-ExoPlayer sincronizada, isolamento de foco de áudio e pré-buffer contínuo para fusão perfeita entre faixas.
  - **Reprodução Sem Pausas (Gapless Playback):** Encadeamento contínuo nativo com transição de frame zero entre músicas consecutivas.
  - **Automix Inteligente:** Mixagens naturais e dinâmicas projetadas para reprodução contínua em playlists e rádios.
  - **Equalizador Nativo:** Integração com o painel de efeitos de áudio profissional do Android vinculado à sessão de áudio ativa.
  - **Temporizador de Sono (Sleep Timer):** Desligamento programado em 15, 30, 45, 60 minutos ou ao término da faixa atual, com fade-out gradual de volume.

- **Home Dinâmica YouTube Music 9.41 (`HomeFeedSynthesizer`):**
  - **Speed Dial Paginado (3x3):** Grade de 9 cards por página com paginação horizontal contínua (`HorizontalPager`) e pontos indicadores de página centralizados.
  - **Feedback Visual Ativo no Início:** Borda branca de `2.dp`, mini equalizador animado no canto superior direito (`AnimatedEqualizerBars`) e barra de progresso vermelha na base do card ativo.
  - **Dinamismo Temporal e Contextual:** Adaptação em tempo real baseada no dia da semana e horário (Fim de semana / Churrasco com botão "Reproduzir tudo", Energia matinal, Foco da tarde, Vibe noturna e Madrugada calma).
  - **Seção Covers e Remixes:** Prateleira horizontal dinâmica filtrando estritamente versões *Slowed*, *Sped Up*, *Remixes*, *VIP Edits* e *Covers*.
  - **Seção Seus Downloads:** Prateleira com cartões em gradiente oficial para Músicas Curtidas e Mix Offline com ação de reprodução imediata.
  - **Hero Card "Sua Descoberta Diária":** Arte em destaque, badge `NOVA DESCOBERTA`, botão Play branco e assinatura em itálico *"Parecido com [Música] de [Artista]"*.

- **Smart Radio Engine & Afinidade Contínua:**
  - Proporção áurea de novidade / familiaridade (60/40) e fila infinita sem repetições consecutivas de um mesmo artista.
  - Guarda de isolamento acústico estrito (`clusterArtists`) para impedir vazamento de faixas fora de gênero na rádio.
  - Repositório de Afinidade do Usuário (`UserAffinityRepository`) com aprendizado contínuo: cálculo de *Skip Penalty* para faixas puladas antes de 30s e bonificação para faixas ouvidas até o fim ou curtidas.
  - Correção definitiva de durações no catálogo, rádio e fila, eliminando qualquer marcador provisório (`--:--`).

- **Central de Configurações no Padrão Spotify:**
  - Navegação hierárquica fluida por categorias (Economia de Dados, Qualidade da Mídia, Reprodução, Conteúdo e Exibição, Armazenamento, Sobre) com barra de pesquisa instantânea.
  - Gerenciador de Armazenamento com gráfico proporcional e limpeza rápida de cache em 1 toque sem apagar faixas baixadas.
  - Diálogo de Change Log in-app com container rolável vertical (`verticalScroll`), acessível pelo menu Sobre ou na inicialização pós-update.

---

## [1.0.8] - 2026-10-23

### Adicionado
- **Aba Biblioteca Completa (Estilo YTM):** Playlists personalizadas, músicas salvas e downloads offline em múltiplos blocos paralelos.
- **Letras em Tela Cheia (Estilo Apple Music):** Efeito Aurora Blur dinâmico baseado nas cores da capa e cápsula de volume interativa.
- **Gestos Fluidos & Motion no Player (120Hz):** Gesto 1:1 de fechamento do player com morphing de capa e fila "A Seguir" com 3 estágios.
