# 🎵 NERF Music v1.4.2 — Onboarding Elegante, Menu de Opções & JAM Refinada! 🚀

A versão **v1.4.2** traz uma reformulação visual completa focada em fluidez e elegância: nova experiência de boas-vindas com personalização de artistas no estilo Apple a 120Hz, menu oficial de opções da faixa com os 3 pontinhos em todas as telas, e aprimoramento ergonômico no painel da NERF JAM com Glassmorphism refinado e correções na fila colaborativa.

---

### 🔥 Destaques Principais da Versão 1.4.2

#### 1. ✨ Onboarding Elegante & Personalização de Primeiro Acesso
- **Experiência Imersiva Apple-like (`OnboardingScreen`):**
  - Apresentação visual limpa com tipografia SF Pro, animações suaves e introdução dos principais recursos (Sem anúncios, Karaokê DSP, NERF Jam e Alta Fidelidade).
- **Seleção Dinâmica de Artistas Favoritos:**
  - Grade interativa de seleção de artistas com busca em tempo real e catálogo visual atualizado, configurando instantaneamente as afinidades e prateleiras personalizadas da Home.
- **Micro-interações com Física Real (`applePressFeedback`):**
  - Modificador de toque com amortecimento e desaceleração física Apple, sem efeito ripple cinza genérico do Material Design.
- **Suporte Nativo a Telas 120Hz (LTPO):**
  - Configuração do modo de exibição preferencial na inicialização da `MainActivity` para garantir a taxa máxima de 120Hz em telas dinâmicas (ex.: Samsung Galaxy S24+).

#### 2. 🎵 Menu Oficial de Opções da Faixa (3 Pontinhos)
- **Painel Completo de Ações (`TrackOptionsBottomSheet`):**
  - BottomSheet oficial no padrão YouTube Music integrado globalmente na Pesquisa, Home, Álbum e Artista.
  - Acesso direto com 1 toque a *"Tocar a seguir"*, *"Iniciar rádio"*, *"Adicionar à fila"*, *"Salvar na playlist"*, *"Curtir/Desfavoritar"*, *"Download granular"*, *"Compartilhar"* e navegação para artista e álbum.

#### 3. 🎧 Painel NERF JAM com Design Glassmorphic & Ergonomia
- **Linguagem Visual Glassmorphism:**
  - Cartões translúcidos com bordas sutis e cantos arredondados de 22.dp, botões em cápsula e novos interruptores com física contínua estilo iOS.
- **Ergonomia e Rolagem Aprimorada:**
  - Janela com altura proporcional de 85%, permitindo rolagem vertical suave sem fechar acidentalmente com gestos leves.
  - Botão *"Start Jam"* e seletor *"Avançado / LAN"* com espaçamento balanceado.
  - Remoção de elementos visuais redundantes e suavização da pulsação do ícone de ondas para um ciclo natural de 2.2 segundos.
- **Estabilidade da Fila Colaborativa na JAM:**
  - Correção de exceções `IndexOutOfBoundsException` ao reordenar, remover e selecionar faixas como convidado, com delegação segura e em tempo real para o Host.

---

### 📦 Informações Técnicas da Compilação
- **Versão:** `1.4.2` (VersionCode: `21`)
- **Target SDK:** Android 14 (API 34)
- **Min SDK:** Android 8.0 (API 26)
- **Assinatura:** Assinado com a chave oficial de Release (`release.jks`) via pipeline seguro do GitHub Actions.
