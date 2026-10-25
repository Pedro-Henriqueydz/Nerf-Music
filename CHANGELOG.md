# Registro de Alterações (Changelog) - NERF Music

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo seguindo as convenções de [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/) e [Semantic Versioning](https://semver.org/lang/pt-BR/).

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
