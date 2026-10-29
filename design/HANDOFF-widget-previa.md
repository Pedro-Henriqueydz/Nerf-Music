# NERF Music — widget e prévia de compartilhamento

Documento visual: `NerfMusic-widget-share.figcraft`. Exportações: `widget-final.png` e `previa-final.png`. Os layouts representam a direção aprovada para implementação; as capas e o texto são conteúdo de demonstração.

## Widget expandido

- Referência: 440 × 326 dp, raio externo de 28 dp e margem interna de 20 dp. Fundo com gradiente derivado da cor dominante da capa, preservando contraste de texto e controles.
- Capa atual: 94 × 94 dp, raio de 14 dp. Título e artista começam em x = 130 dp. Truncar em uma linha; manter capa e metadados alinhados pelo topo.
- Progresso: trilho de 400 × 4 dp, x = 20 dp, y = 123 dp. Tempo decorrido à esquerda e duração à direita. Atualizar enquanto a música toca.
- Controles: uma linha com **Curtir, Anterior, Reproduzir/Pausar, Próxima**. Centros na referência: 49, 160, 280 e 391 dp. Usar `spaceBetween` entre áreas de toque de pelo menos 54 × 54 dp, com 22 dp de margem lateral; em larguras diferentes, preservar margens e distribuir o espaço livre. Os botões secundários têm disco translúcido `#FFFFFF1A`, contorno `#FFFFFF28` de 1 dp e ícone branco. A ação principal tem disco branco de 60 dp, ícone escuro e sombra suave. O coração preenchido representa estado curtido; quando descurtido, usar contorno. **Não incluir repetição/loop.**
- Recomendações: cinco capas quadradas de 75 × 75 dp na referência, raio de 12 dp, separação de 6 dp e margens de 20 dp. Ajustar as cinco larguras ao espaço disponível, mantendo proporção 1:1 e exibindo toda a capa. Não cortar a primeira ou a última capa nas bordas do widget. Cada capa abre a respectiva faixa.
- Estados: alternar ícone Play/Pause e coração imediatamente ao toque; exibir progresso e tempos atualizados; fornecer imagem de fallback e manter as cinco capas em cache. Áreas de toque e semântica acessível devem corresponder ao controle visual.

## Prévia de compartilhamento

- Referência: viewport de 390 × 900 dp. Abrir como **folha modal sobre o player**, com scrim sobre a capa, cantos superiores de 28 dp, fundo em gradiente `#222229 → #15151A → #09090B`. Esta composição segue a família visual da folha de JAM e dos controles de download, letra e fila: discos translúcidos, borda branca fina, espaçamento de 20 dp e hierarquia tipográfica clara.
- Cabeçalho: ícone circular de 46 dp, título de 20 sp, descrição de 12 sp e fechar circular de 36 dp. Separador fino antes da faixa. Linha da faixa com capa de 44 dp, nome, artista e estado de imagem pronta.
- Card: prévia central em **9:16**, 172 × 306 dp no mockup, raio de 18 dp, imagem da capa, marca no topo, gradiente inferior, título, artista e chamada para ouvir. Exportar o card em 1080 × 1920 px. O card visto na prévia deve corresponder à imagem exportada.
- Estilos: três escolhas de texto, `Vidro`, `Minimal` e `Capa inteira`, em caixas de 112 × 58 dp. Seleção marcada por contorno laranja `#FF7135` de 1,5 dp e fundo laranja translúcido. Não usar miniaturas dentro desses botões: ao trocar de estilo, atualizar a prévia real e manter o estilo selecionado até o compartilhamento.
- Ações: `Stories`, `WhatsApp` e `Salvar`, em três colunas igualmente distribuídas. Disco de 56 dp, rótulo abaixo, Instagram destacado com `#FF7135`. Abaixo, botão de largura total `Copiar link da música`; manter área segura inferior e indicador de gesto. Desabilitar ações enquanto a imagem é gerada e mostrar progresso sem deslocar o layout.
- Fluxos: Stories envia imagem de fundo e adesivo de link quando disponível; WhatsApp compartilha imagem e link; Salvar grava a mesma imagem da prévia na galeria; Copiar link confirma a cópia. Falhas devem manter a folha aberta e oferecer uma mensagem curta com tentativa novamente.
- Acessibilidade: áreas de toque de pelo menos 48 dp, descrições de ações, foco ordenado de cima para baixo e contraste legível sobre capa dinâmica. Em telas baixas, a folha rola internamente e a área de ações continua alcançável.

## Revisão visual

| Antes | Depois | Por quê |
|---|---|---|
| Controles agrupados no centro | Quatro discos distribuídos até as margens, com ação principal destacada | A linha fica equilibrada e mais fácil de tocar. |
| Capas percebidas como cortadas e título ambíguo | Cinco quadrados visíveis, proporcionais, sob “Recomendado para você” | A recomendação fica reconhecível e consistente. |
| Prévia como tela genérica | Folha modal com materiais, cantos e botões da família do player | O compartilhamento parece parte do NERF Music. |
| Estilos indicados por elementos decorativos | Botões de texto e card atualizado pela seleção | A escolha fica simples e a prévia se torna a fonte de verdade. |

Nenhum código do aplicativo foi alterado nesta revisão de design.
