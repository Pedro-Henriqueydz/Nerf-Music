# 🚀 NERF Music v1.1.0 — Dynamic YouTube Music 9.41 Home & Smart Radio Engine

Release técnica com foco em arquitetura reativa, orquestração local de feeds personalizados e motor de rádio com isolamento acústico e afinidade contínua.

---

### 🏛️ Arquitetura do Feed Dinâmico (`HomeFeedSynthesizer`)
- **Orquestração Local de Conteúdo:** Contorna a limitação do endpoint anônimo `FEmusic_home` do InnerTube sintetizando prateleiras dinâmicas a partir da fusão de `UserHistoryRepository`, `LibraryRepository` (Playlists e Curtidas), `UserAffinityRepository` e queries contextuais do catálogo.
- **Speed Dial 3x3 Paginado:**
  - Componente `YtmSpeedDialPagerSection` implementado com Jetpack Compose `HorizontalPager` e `rememberPagerState`.
  - Grade estrita de 3 linhas por 3 colunas (9 cartões por página) com cálculo de chunks e dots indicadores centralizados.
  - Cartão `YtmSpeedDialCard` com proporção 1:1, cantos de 8dp, scrim vertical escuro na base, borda branca de `2.dp` para a faixa ativa, mini equalizador animado (`AnimatedEqualizerBars`) e barra de progresso de reprodução em vermelho (`YtmRed`).
  - Suporte a atalhos de sistema: "Músicas Curtidas" com gradiente tricolor (`#8A2387`, `#E94057`, `#F27121`) e playlists locais do usuário.
- **Heurística Temporal Contextual (Time-of-Day):**
  - Detecção dinâmica baseada no relógio do sistema:
    - Fim de semana (Sexta 17h até Domingo 23h59): busca contextual para "Para o fim de semana / Churrasco".
    - 05h-11h: "Energia para o seu dia".
    - 12h-17h: "Foco e ritmo".
    - 18h-23h: "Para desacelerar".
    - 00h-05h: "Madrugada calma".
- **Prateleira Covers & Remixes:**
  - Query especializada combinada com filtragem léxica heurística (`remix`, `slowed`, `sped`, `cover`, `reverb`, `vip`, `edit`) isolada para o gênero de preferência do usuário.
- **Seção Seus Downloads & Mix Offline:**
  - Renderização de cartões com gradientes vetoriais oficiais para Músicas Curtidas e Mix Offline com ação de reprodução imediata de lote via `MusicViewModel.handleHomeTrackClick`.
- **Hero Card "Sua Descoberta Diária":**
  - Consulta ao rádio da melhor seed do usuário via `catalogClient.getUpNextRadio`, selecionando candidatos desconhecidos e exibindo badge `NOVA DESCOBERTA` e selo de afinidade em itálico *"Parecido com [Música] de [Artista]"*.

---

### 🧠 Smart Radio Engine & Afinidade Acústica
- **Motor de Recomendação Inteligente:**
  - Proporção áurea de novidade / familiaridade: 60% de descoberta dentro do mesmo cluster sonoro e 40% de familiaridade baseada no histórico.
  - Fila infinita contínua sem repetições consecutivas de um mesmo artista.
- **Isolamento de Cluster Sonoro:**
  - Guard rail acústico (`clusterArtists.contains(...)`) para evitar poluição da rádio com gêneros desconexos (ex: rock clássico em rádios de wave/phonk).
- **Repositório de Afinidade do Usuário (`UserAffinityRepository`):**
  - Registro de eventos de ciclo de vida (`recordTrackEnded`): cálculo de *Skip Penalty* para faixas puladas antes de 30s (ou antes de 40% da duração total) e incremento de score para faixas ouvidas por completo ou curtidas.
  - Penalização de frequência para evitar saturação de faixas frequentes.

---

### ⏱️ Resolução Resiliente de Durações
- **InnerTubeCatalogClient:** Extração abrangente de tempos reais a partir de múltiplos nós de resposta (`simpleText`, `runs`, `accessibilityData` em PT e EN).
- **Timeline e SharedPreferences:** Enriquecimento contínuo via `MusicViewModel.syncTimelineState` e `UserHistoryRepository.updateTrackDuration`, eliminando definitivamente valores provisórios (`--:--`) da fila, player e histórico.

---

### 📦 Metadados da Versão
- **Versão:** `v1.1.0`
- **VersionCode:** `10`
- **Target SDK:** Android 14 (API 34)
- **Min SDK:** Android 8.0 (API 26)
