# 🎵 NERF Music v1.4.0 — O Maior Lançamento da História do App! 🚀

A versão **v1.4.0** transforma o NERF Music em uma das experiências de streaming e karaokê mais completas e inovadoras para Android. Com foco em sincronização social em tempo real, inteligência de letras avançada e arquitetura de áudio profissional, esta atualização traz funcionalidades revolucionárias desenvolvidas sem requerer qualquer conta de usuário ou login (`no-account architecture`).

---

### 🔥 Destaques Principais da Versão 1.4.0

#### 1. 🎤 Modo Karaokê de Próxima Geração & Troca Instantânea por Instrumental Oficial
- **Troca Instantânea por Instrumental Oficial (Dual-ExoPlayer Crossfade):**
  - Ao carregar qualquer música, o sistema consulta em segundo plano versões oficiais instrumentais/karaokê no catálogo (`$title $artist official instrumental`), validando a compatibilidade de duração ($\pm 8\text{s}$) e mantendo uma instância secundária de `ExoPlayer` (`instrumentalPlayer`) pré-aquecida e pronta no buffer de memória.
  - Ao ativar o Modo Karaokê no botão flutuante de microfone (🎤), o aplicativo realiza uma transição cruzada equal-power instantânea de $\sim 120\text{ms}$ para o áudio instrumental no milissegundo exato da reprodução, mantendo o áudio original atrelado ao slider vocal como guia opcional para o cantor.
- **Isolamento Vocal DSP Aprimorado (Filtro Biquad Butterworth não abafado):**
  - Para faixas que não possuem instrumental oficial disponível, o processador de áudio local (`VocalIsolationAudioProcessor`) implementa atenuação seletiva de canal central na faixa de $280\text{ Hz} - 3500\text{ Hz}$ com curva de ganho de presença (`clarityGain`), mantendo o peso dos graves, corpo da caixa de bateria e o brilho dos agudos sem sensação de áudio abafado.
- **Destaque Palavra por Palavra Condicional:**
  - O preenchimento dinâmico palavra por palavra das letras sincronizadas (LRCLIB) e a contagem regressiva instrumental (`• • •`) estilo Apple Music Sing passam a ser ativados estritamente quando o usuário engaja o Modo Karaokê.

#### 2. 🌍 Tradução e Pronúncia Apple Music Glass (`emil-design-eng`)
- **Tradução Linha a Linha Multilíngue:**
  - Tradução em tempo real de versos sincronizados com seletor flexível de idiomas (`Auto`, `PT`, `EN`, `ES`, `FR`, `JA`, `KO`, `DE`, `IT`). O modo automático segue as preferências do sistema operacional com fallback seguro.
- **Guia Fonético / Pronúncia Romanizada:**
  - Mapeamento fonético e romanizado de caracteres internacionais (incluindo hiragana, katakana, kanji, hangul e cirílico) para permitir cantar e acompanhar faixas em qualquer idioma.
- **Suporte Abrangente a Letras de Fallback (YouTube Music / Musixmatch):**
  - Mesmo quando a música possui apenas letras estáticas (`plainLyrics`) sem sincronia por linha, o motor decompõe os versos preservando pronúncia e tradução linha a linha.
- **Apple Glass Popover UI:**
  - Interface moderna com estética Apple Music / iOS 18: menu popover translúcido dark glass (`#1C1C1E` @ 94% alpha), bordas especulares ultrafinas, animação origin-aware com curva `CubicBezierEasing(0.23f, 1f, 0.32f, 1f)` (<200ms), feedback de clique táctil (`scale(0.97f)`) e opção para ocultar o verso original mantendo apenas a tradução ou pronúncia.

#### 3. 🎧 NERF JAM — Escuta Sincronizada em Grupo (Estilo Spotify Jam)
- **100% No-Account & Descentralizado:**
  - Sessões de escuta coletiva em tempo real sem qualquer necessidade de cadastro, autenticação ou login.
  - Hospedagem local de WebSocket server incorporado (`JamWebSocketServer` na porta `8924`) para conexões diretas via rede local / Wi-Fi e relay de fallback para conexões remotas.
- **Convites Fluidos via DeepLinks e QR Code:**
  - DeepLink universal `nerfmusic://jam?id=...&host=...&relay=...` compatível com QR Code dinâmico renderizado nativamente na tela para entrada instantânea com a câmera ou leitor.
- **Sincronia Total de Estado:**
  - Transmissão instantânea de Play, Pause, Seek (com compensação de latência RTT em janelas de 350ms), transições de faixa e manipulação colaborativa da fila (`ADD_TO_QUEUE`, `REMOVE_QUEUE_ITEM`, `MOVE_QUEUE_ITEM`).

#### 4. ⚡ Otimizações do Player, Performance e Downloads Granulares
- **Eliminação de Delay e Flickering no Swipe do Player:**
  - Otimismo visual antecipado (`pendingSwipeTrack`), pré-aquecimento de capas na memória do Coil e comutação instantânea de `MediaSession` no `AudioPlayerService` eliminando o atraso anterior de 2.5 a 5 segundos ao trocar de faixa.
- **Configurações Granulares de Download:**
  - Opções independentes para baixar áudio, vídeos de fundo (Canvas) e letras de forma desacoplada, com cálculo dinâmico do consumo em disco.
- **Empty State Offline & Reconexão Automática:**
  - Interface nativa informando modo offline e monitor de conectividade reativo (`ConnectivityObserver`) que restaura o feed da Home instantaneamente assim que a internet retorna, com suporte nativo a Pull-to-Refresh.
- **Fechamento Limpo do Serviço (`onTaskRemoved`):**
  - Desligamento estrito do serviço em primeiro plano e remoção da notificação ao encerrar o app na tela de aplicativos recentes.

---

### 📦 Informações Técnicas da Compilação
- **Versão:** `1.4.0` (VersionCode: `19`)
- **Target SDK:** Android 14 (API 34)
- **Min SDK:** Android 8.0 (API 26)
- **Assinatura:** Assinado com a chave oficial de Release (`release.jks`) via pipeline seguro do GitHub Actions.
