# Registro de Alterações (Changelog) - NERF Music

Todas as mudanças notáveis deste projeto serão documentadas neste arquivo seguindo as convenções de [Keep a Changelog](https://keepachangelog.com/pt-BR/1.0.0/) e [Semantic Versioning](https://semver.org/lang/pt-BR/).

---

## [1.1.0] - 2026-10-24

### Adicionado
- **Home Dinâmica YouTube Music 9.41 (Paridade Visual e Comportamental):**
  - **Speed Dial Paginado (3x3):** Grade de 3 colunas por 3 linhas (9 cards por página) com paginação horizontal contínua (`HorizontalPager`) e pontos indicadores de página centralizados.
  - **Integração de Bibliotecas no Speed Dial:** Inclusão automática das playlists criadas pelo usuário, atalho exclusivo de "Músicas Curtidas" com gradiente magenta/laranja e músicas recentes filtradas por afinidade acústica.
  - **Feedback Visual Ativo no Speed Dial:** Borda branca de destaque de `2.dp`, mini equalizador animado no canto superior direito (`AnimatedEqualizerBars`) e barra de progresso de reprodução em vermelho oficial (`YtmRed`) na base do card ativo.
  - **Dinamismo Temporal e Contextual:** Módulo `HomeFeedSynthesizer` que adapta as prateleiras em tempo real baseado no dia da semana e horário:
    - *Fim de semana:* "Para o fim de semana" (strapline "CHURRASCO, FESTA E DESCONTRAÇÃO") com botão de atalho "Reproduzir tudo".
    - *Manhã:* "Energia para o seu dia".
    - *Tarde:* "Foco e ritmo".
    - *Noite:* "Para desacelerar".
    - *Madrugada:* "Madrugada calma".
  - **Seção Covers e Remixes:** Prateleira horizontal dinâmica com filtro estrito de versões alternativas (*Slowed*, *Sped Up*, *Remixes*, *VIP Edits* e *Covers*) respeitando o gênero musical em reprodução.
  - **Seção Seus Downloads:** Prateleira elegante com cartões em gradiente oficial para "Músicas Curtidas" (`#8A2387` a `#F27121`) e "Mix Offline" (`#4A00E0` a `#8E2DE2`), permitindo reprodução com um único toque.
  - **Hero Card "Sua Descoberta Diária":** Card nobre com arte em destaque, badge `NOVA DESCOBERTA`, botão Play branco e assinatura contextual em itálico *"Parecido com [Música] de [Artista]"*.
  - **Suporte a Ações Rápidas da Home:** `MusicViewModel.handleHomeTrackClick` para redirecionamento inteligente de toques em atalhos de curtidas, downloads locais, playlists do usuário ou faixas individuais.

---

### Smart Radio & Algoritmo (Grupo 1)

### Adicionado
- **Smart Radio Engine (Motor de Rádio e Fila Infinita Premium):**
  - Algoritmo de proporção áurea (60% descoberta do mesmo cluster / 40% familiaridade do usuário).
  - Repositório de Afinidade do Usuário (`UserAffinityRepository`) com aprendizado contínuo:
    - Cálculo de *Skip Penalty* para faixas puladas nos primeiros 30 segundos.
    - Bonificação de afinidade para faixas ouvidas até o fim ou curtidas.
  - Isolamento estrito de cluster acústico (`clusterArtists`) para impedir vazamento de faixas fora de contexto de gênero.
  - Fila inteligente sem repetições consecutivas do mesmo artista.

### Corrigido
- **Resolução de Durações no Catálogo e na Fila:**
  - Extração resiliente de durações reais em milissegundos e texto legível a partir de payloads multilíngues do InnerTube (`simpleText`, `runs`, acessibilidade PT/EN).
  - Eliminação de marcadores provisórios `--:--` na fila do player e no histórico.
  - Preservação e enriquecimento automático de durações cacheadas em SharedPreferences via `UserHistoryRepository.updateTrackDuration`.

---

## [1.5.0] - 2026-10-24

### Adicionado
- **Equalizador Nativo de 5 Bandas:** Suporte integrado aos efeitos de áudio do sistema (`AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL`).
- **Sleep Timer (Temporizador de Suspensão):** Desligamento gradual da música em 15, 30, 45, 60 minutos ou ao término da faixa atual.
- **Gerenciador de Armazenamento e Limpeza de Cache:** Detalhamento do espaço ocupado por capas, cache de áudio e dados do aplicativo com botão de limpeza rápida.
- **Personalização de Aparência:** Modos de visualização em grade ou lista para a Biblioteca, controle de exibição de letras acima do título e seleção de qualidade de áudio.

---

## [1.4.0] - 2026-10-23

### Adicionado
- **Spotify Canvas Integration:** Exibição contínua de loops de vídeo curtos em faixas compatíveis com opção de economia de dados móveis (Wi-Fi apenas).
- **Letras Sincronizadas em Tela Cheia:** Rolagem automática linha a linha com suporte a salto temporal interativo.
- **Transições Suaves de Áudio:** Efeito de crossfade entre faixas e prefetch automático da próxima música na fila.
