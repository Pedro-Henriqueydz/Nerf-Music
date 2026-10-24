# Registro de Alterações (Changelog) - NERF Music

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo seguindo as convenções de [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/) e [Semantic Versioning](https://semver.org/lang/pt-BR/).

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
