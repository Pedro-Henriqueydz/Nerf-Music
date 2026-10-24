# 🚀 NERF Music v1.1.0 — Major Release: Audio Transitions, Dynamic Home, Smart Radio & Advanced Settings

Esta versão consolida grandes avanços de arquitetura, áudio e experiência de usuário, integrando transições contínuas de reprodução, tela inicial dinâmica inspirada no YouTube Music 9.41, motor de rádio com isolamento acústico e nova central de configurações no padrão Spotify.

---

### 🎧 1. Motor de Áudio & Transições de Faixa (`Song Transitions`)
- **Crossfade Suave com Curva de Potência Constante (0s a 12s):**
  - Implementação de arquitetura baseada em dois players concorrentes (`Dual-ExoPlayer Architecture`) com pré-carregamento sincronizado do fluxo da próxima música antes do ponto de transição.
  - Curva senoidal/cossenoidal de atenuação e ganho mútuo (`Equal-Power Crossfade Curve`) para eliminar qualquer alteração perceptível de pressão sonora e artefatos de clique.
  - Isolamento de foco de áudio (`Audio Focus Decoupling`) para evitar que instâncias concorrentes preempcionem a sessão de mídia principal.
- **Reprodução Sem Pausas (Gapless Playback):**
  - Encadeamento contínuo nativo com transição de frame zero entre faixas consecutivas, preservando a integridade de gravações ao vivo e álbuns contínuos.
- **Automix Inteligente:**
  - Heurística de transição rápida e fade dinâmico projetada para fluxos de rádio e playlists em modo contínuo.
- **Integração com Equalizador Nativo:**
  - Acesso transparente aos efeitos de áudio do sistema operacional via `AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL` vinculado ao `audioSessionId` ativo do ExoPlayer.
- **Temporizador de Sono (Sleep Timer):**
  - Gerenciador de suspensão com intervalos de 15, 30, 45, 60 minutos e modo contextual *"Ao término da faixa atual"*, implementando rampa de desaceleração suave de volume (*Volume Ramp Fade-Out*) antes do encerramento para evitar interrupções abruptas.

---

### 🏛️ 2. Home Dinâmica Estilo YouTube Music 9.41 (`HomeFeedSynthesizer`)
- **Síntese e Orquestração Local:**
  - Fusão reativa de metadados entre fontes locais (`UserHistoryRepository`, `LibraryRepository`, `UserAffinityRepository`) e o catálogo anônimo do InnerTube, contornando a esterilidade do browse padrão não autenticado.
- **Speed Dial Paginado (3x3):**
  - Grade 3 colunas x 3 linhas (9 cards por página) implementada via `HorizontalPager` do Jetpack Compose Foundation com pontos indicadores de página (`rememberPagerState`).
  - Cards com proporção 1:1, cantos de 8dp, scrim gradiente escuro de alta legibilidade, borda branca de `2.dp` para a faixa ativa, mini equalizador animado (`AnimatedEqualizerBars`) e barra de progresso em vermelho oficial (`YtmRed`) na base do card.
  - Inclusão automática de atalhos rápidos: Músicas Curtidas (com gradiente magenta/laranja), playlists criadas pelo usuário e faixas do histórico com afinidade positiva.
- **Dinamismo Temporal e Contextual (Time-of-Day):**
  - Avaliação em tempo real pelo relógio do dispositivo:
    - *Fim de semana (Sexta 17h até Domingo 23h59):* "Para o fim de semana" / "CHURRASCO, FESTA E DESCONTRAÇÃO" com botão de ação rápida "Reproduzir tudo".
    - *Manhã (05h-11h):* "Energia para o seu dia".
    - *Tarde (12h-17h):* "Foco e ritmo".
    - *Noite (18h-23h):* "Para desacelerar".
    - *Madrugada (00h-05h):* "Madrugada calma".
- **Prateleira Covers & Remixes:**
  - Query especializada combinada com filtragem léxica heurística (`remix`, `slowed`, `sped`, `cover`, `reverb`, `vip`, `edit`) restrita ao gênero e cluster de preferência do usuário.
- **Seus Downloads & Mix Offline:**
  - Cartões com gradientes vetoriais oficiais para Músicas Curtidas e Mix Offline prontos para execução imediata em lote.
- **Hero Card "Sua Descoberta Diária":**
  - Consulta ao rádio da seed favorita via `InnerTubeCatalogClient.getUpNextRadio`, destacando faixas desconhecidas com badge `NOVA DESCOBERTA` e selo em itálico *"Parecido com [Música] de [Artista]"*.

---

### 🧠 3. Smart Radio Engine & Afinidade Acústica Contínua
- **Proporção Áurea de Rádio:**
  - Equilíbrio matemático de 60% novidades dentro do mesmo cluster sonoro e 40% familiaridade do histórico do usuário, sem repetições consecutivas de um mesmo artista.
- **Guarda de Isolamento Acústico:**
  - Validação de fronteira de cluster (`clusterArtists.contains(...)`) para erradicar contaminação de gêneros desconexos na fila do rádio.
- **Repositório de Afinidade do Usuário (`UserAffinityRepository`):**
  - Cálculo de *Skip Penalty* para faixas puladas precocemente (< 30s ou < 40% de reprodução) e bonificação exponencial para músicas ouvidas até o fim ou favoritadas.
- **Resolução e Cura de Durações:**
  - Extração de durações exatas a partir de nós multilíngues do InnerTube (`simpleText`, `runs`, acessibilidade PT/EN).
  - Enriquecimento contínuo de metadados em runtime, eliminando placeholders provisórios (`--:--`) da fila, player e histórico.

---

### ⚙️ 4. Central de Configurações & Armazenamento (Padrão Spotify)
- **Navegação Hierárquica & Busca Global:**
  - Reestruturação completa em sub-telas temáticas (Economia de Dados, Qualidade da Mídia, Reprodução, Conteúdo e Exibição, Armazenamento, Sobre) com barra de pesquisa instantânea de configurações.
- **Gerenciador de Armazenamento (`StorageSettingsScreen`):**
  - Gráfico de barras proporcional no padrão Spotify detalhando o consumo de espaço em disco: Músicas Baixadas, Cache de Streaming e Dados do Aplicativo.
  - Ação de limpeza de cache em um toque (`clearAppCache`) com atualização dinâmica dos contadores em MB/GB sem afetar downloads.
- **Modal de Novidades In-App Acessível:**
  - Diálogo de Change Log voltado ao usuário final com container rolável vertical (`verticalScroll`), garantindo leitura completa em qualquer resolução de tela.

---

### 📦 Metadados da Versão
- **Versão:** `v1.1.0`
- **VersionCode:** `10`
- **Target SDK:** Android 14 (API 34)
- **Min SDK:** Android 8.0 (API 26)
