# 🎵 NERF Music v1.4.1 — Correções, Precisão de Karaokê & JAM Global! 🚀

A versão **v1.4.1** aprimora a experiência introduzida na v1.4.0 com correções fundamentais de precisão no casamento de faixas instrumentais, alinhamento automático de introduções musicais via correlação de transientes, refinamento acústico do isolamento vocal DSP anti-abafamento, conectividade Global pela internet para a NERF JAM via WebSocket/MQTT e governança completa da sala pelo Host.

---

### 🔥 Destaques Principais da Versão 1.4.1

#### 1. 🎤 Karaokê de Alta Precisão & DSP Anti-Abafamento
- **Casamento Estrito de Instrumentais Oficiais (`selectBestInstrumentalCandidate`):**
  - Eliminação de correspondências errôneas e remixes desconexos (ex.: *"Reason - Altare"* buscando faixas incorretas). Validação estrita de tolerância de duração ($\le 12\text{s}$) e pontuação ponderada de títulos e artistas oficiais.
- **Alinhamento Automático de Beat e Sincronia de Entrada (`computeBestBeatOffsetMs`):**
  - Correção de atrasos de introdução em músicas como *"É Com Ela Que Eu Estou - Cristiano Araújo"*. O motor analisa os envelopes de transientes do áudio principal e do instrumental para compensar o atraso inicial no playback em tempo real.
- **Isolamento Vocal DSP Aprimorado (Filtro Mid-Side Anti-Abafamento):**
  - Reformulação do processador de áudio local: preservação total de subgraves, bateria e estéreo através de filtragem Mid-Side centrada estritamente na banda formante da voz ($280\text{ Hz} - 3500\text{ Hz}$) com ganho de presença, proporcionando uma transição linear e natural no slider de volume vocal.

#### 2. 🎧 NERF JAM Global & Governança da Sala pelo Host
- **Conectividade Global via Internet (4G / 5G / Wi-Fi):**
  - A JAM conecta ouvintes em qualquer lugar do mundo por meio de transporte MQTT sobre WebSockets com TLS (`wss://broker.emqx.io:8084/mqtt` ou relay dedicado), com códigos de sala curtos (ex.: `NERF-4821` ou `4821`) e DeepLinks universais.
- **Governança Completa da Sala para o Host (`JamRoomSettings`):**
  - **Permissão de Fila Colaborativa:** Controle se convidados podem adicionar, remover ou reordenar itens na fila.
  - **Permissão de Controle de Reprodução:** Controle se convidados podem pular faixas, pausar, dar play ou selecionar músicas da fila.
  - **Looping Sincronizado da Sala:** Modo de repetição unificado (`Desativado`, `Repetir Fila`, `Repetir 1`) sincronizado entre todos os participantes.
  - **Crossfade Sincronizado do Host (0s a 12s):** O tempo de Crossfade e Automix configurado pelo Host é aplicado automaticamente a todos os convidados durante a sessão, sem sobrescrever suas configurações pessoais locais.
  - **Karaokê em Grupo & Kick:** Sincronização do modo karaokê na sala e opção de remover participantes indesejados.
- **Nova Identidade Visual e Acesso Rápido na Home:**
  - Novo ícone customizado da JAM (`JamPersonWavesIcon`) com silhueta humana e ondas sonoras estilo Spotify.
  - Botão de acesso rápido à JAM na barra superior da Home (`YtmTopBar`) ao lado das configurações.

---

### 📦 Informações Técnicas da Compilação
- **Versão:** `1.4.1` (VersionCode: `20`)
- **Target SDK:** Android 14 (API 34)
- **Min SDK:** Android 8.0 (API 26)
- **Assinatura:** Assinado com a chave oficial de Release (`release.jks`) via pipeline seguro do GitHub Actions.
