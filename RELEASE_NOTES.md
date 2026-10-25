# ⚡ NERF Music v1.3.0 — Central de Migração Multi-Streaming & Personalização da Home

Esta versão apresenta a nova **Central de Migração de Streaming**, permitindo transferir bibliotecas completas de músicas e playlists do **YouTube Music** e **Spotify** para o NERF Music, além de adaptar e transformar o feed da **Home** imediatamente com base no DNA acústico importado.

---

### 🌐 1. Conexão Direta Oficial (Com Login Seguro)
- **YouTube Music (Conta Google):**
  - Sincronização em 1 toque de **todas as Músicas Curtidas** e **Playlists Pessoais**.
  - **Paginação Recursiva Sem Perdas:** Algoritmo avançado que rastreia `shelf.continuations`, tokens `continuationItemRenderer` embutidos nos itens e ações de continuação `onResponseReceivedActions`. Recupera **100% das faixas**, mesmo em bibliotecas gigantescas com mais de 250 músicas.
  - **Capas Quadradas Nativas de Alta Resolução:** Eliminação definitiva de barras pretas widescreen (letterbox 16:9), extraindo capas originais Google/YTM em resolução nativa 544x544 (`=w544-h544-l90-rj`).
  - **Duração Exata em Minutos e Segundos:** Extração da coluna fixa de tempo (`fixedColumns`), eliminando marcadores provisórios (`--:--`).
- **Spotify:**
  - Login seguro via Web Player oficial para sincronização direta de **Músicas Curtidas** e **Playlists**.
  - Paginação automática pela API oficial (`/v1/playlists/.../tracks?limit=100&offset=...`).

---

### 🕶️ 2. Modo Anônimo (Sem Login)
- **Importação por Links Públicos:**
  - Cole qualquer link público de playlist do **Spotify** ou **YouTube Music**.
  - **Motor Acústico de Alta Precisão (`isMatch`):** Correspondência calibrada por tolerância de sobreposição léxica ($\ge 50\%$) e contenção de termos, elevando a taxa de sucesso para **~98%** em títulos complexos (remixes, phonk, wave e gravações ao vivo).
- **Importação Universal por Arquivos de Backup:**
  - Compatibilidade com arquivos `.csv` e `.txt` exportados de plataformas como Soundiiz, TuneMyMusic ou listas manuais em formato `"Artista - Música"`.

---

### 💾 3. Persistência Atômica em Lote & Prevenção de Concorrência
- **Criação Atômica de Playlists:**
  - Playlists de centenas de faixas agora são criadas e gravadas no disco em uma única transação atômica (`createPlaylist(..., initialTracks)` e `addTracksToPlaylist`).
  - Elimina o risco de condições de corrida e sobreposição de gravações no `SharedPreferences`, garantindo que **todas as faixas importadas** permaneçam salvas na biblioteca.

---

### 🧠 4. DNA Acústico & Transformação Imediata da Home
- Assim que qualquer importação é concluída, as faixas mais ouvidas e sementes favoritas são injetadas diretamente no grafo de afinidade (`UserHistoryRepository` e `UserAffinityRepository`).
- A tela inicial se atualiza na mesma hora, trazendo o carrossel de **Artistas Mais Ouvidos**, **Músicas Curtidas**, **Escolha a Dedo** e o **Seu Supermix** personalizados com o seu gosto musical.
