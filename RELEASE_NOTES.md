# NERF Music v1.4.5 — Correção do widget expandido

Esta versão restaura o tamanho mínimo necessário para exibir o layout completo do widget expandido no One UI e mantém a atualização após redimensionamento.

### Correções

- **Widget expandido:** `minHeight` volta a `216dp` e `targetCellHeight` a `3`, evitando que o launcher selecione a variante compacta no tamanho padrão.
- **Redimensionamento:** o provider atualiza o `RemoteViews` ao receber `onAppWidgetOptionsChanged` e seleciona o layout correspondente às dimensões atuais.

### Informações da compilação

- Versão: `1.4.5` (versionCode `24`)
- Target SDK: Android 14 (API 34)
- Min SDK: Android 8.0 (API 26)
- APK de release: assinado pelo pipeline do GitHub Actions com os segredos de assinatura configurados no repositório.