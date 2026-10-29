# NERF Music v1.4.3 — Widgets, compartilhamento social e áudio

Esta versão adiciona um fluxo completo de criação e compartilhamento de cards sociais, novos formatos de widgets e normalização de loudness em tempo real. Também corrige atualizações visuais dos widgets e otimiza rotinas do pipeline de áudio.

### Destaques

- **Share Cards:** prévia interativa com variantes de layout, renderização vertical 1080×1920, exportação para WhatsApp, Instagram Stories e galeria; suporte a seleção contínua de até quatro linhas de letras.
- **Widgets de player:** layouts 2×1, 3×1 e 4×3; atualização de progresso e metadados durante a reprodução; paleta derivada da capa; cache persistente de artwork e recomendações; ações de reprodução, curtir e repetir no layout expandido.
- **Atualizações sem flicker:** ações do player usam atualização parcial de `RemoteViews` para preservar artwork e estado visual.
- **Loudness normalization:** processador DSP em streaming com perfis de alvo e limitador true-peak.
- **Qualidade e manutenção:** testes unitários/instrumentados para regras de compartilhamento, cores e cache de capas; remoção de artefatos locais do Graphify.

### Informações da compilação

- Versão: `1.4.3` (versionCode `22`)
- Target SDK: Android 14 (API 34)
- Min SDK: Android 8.0 (API 26)
- APK de release: assinado pelo pipeline do GitHub Actions com os segredos de assinatura configurados no repositório.