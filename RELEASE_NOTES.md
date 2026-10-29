# NERF Music v1.4.4 — Correções e ajustes nos widgets

Esta versão corrige as dimensões do widget expandido e finaliza o ajuste dos controles nos formatos 2x1 e 3x1.

### Alterações

- **Widget expandido:** restaura `minHeight=100dp` e `targetCellHeight=2`, mantendo a possibilidade de redimensionamento. `ExpandedPlayerWidgetProvider` agora atualiza o `RemoteViews` em `onAppWidgetOptionsChanged`, selecionando o layout adequado após redimensionar.
- **Widget 3x1:** título/artista passam para 14sp/12sp; botão de avançar passa para 42dp, alinhado ao play/pause.
- **Widget 2x1:** remove o botão de avançar e seu `PendingIntent`; mantém somente play/pause.

### Informações da compilação

- Versão: `1.4.4` (versionCode `23`)
- Target SDK: Android 14 (API 34)
- Min SDK: Android 8.0 (API 26)
- APK de release: assinado pelo pipeline do GitHub Actions com os segredos de assinatura configurados no repositório.