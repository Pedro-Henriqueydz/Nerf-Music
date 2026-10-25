# 🎵 NERF Music v1.3.2 — Reprodução e migração mais confiáveis

Esta versão corrige problemas de reprodução e torna a importação de bibliotecas mais previsível.

### Correções e melhorias

- **Resolução de áudio:** adiciona uma segunda fonte de resolução quando a primeira não encontra um stream e evita reutilizar streams armazenados para outra qualidade ou conexão.
- **Migração de playlists:** atualiza playlists importadas pela identidade da fonte, evitando cópias a cada nova importação.
- **Músicas curtidas:** adiciona faixas importadas em lote e evita duplicatas.
- **Sessões conectadas:** só confirma a conexão após salvar a sessão com sucesso; falhas de armazenamento seguro não são tratadas como sucesso.
- **Durações:** torna a leitura da duração mais tolerante a respostas incompletas do catálogo.
- **Testes:** adiciona cobertura para a identidade de playlists importadas e a resolução padrão de streams.

---

# 🛡️ NERF Music v1.3.1 - Atualização de Segurança e Estabilidade

Esta versão traz importantes aprimoramentos de segurança, proteção de dados e melhorias de estabilidade em todo o aplicativo.

---

### ✨ Novidades e Melhorias

- **Proteção Avançada de Dados Locais:**
  - Implementação de criptografia autenticada de ponta a ponta (AES-256 GCM integrado ao Android KeyStore) para dados e sessões de streaming locais.
  - Bloqueio de exportação indevida de dados da aplicação via backups não autorizados.

- **Conexões e Navegação Segura:**
  - Implementação de política restrita de tráfego HTTPS em todos os módulos de rede.
  - Isolamento e whitelist de domínios seguros durante os fluxos de login no WebView.

- **Integridade de Atualizações:**
  - Validação aprimorada de integridade de arquivos e verificação estrutural de pacotes no In-App Updater.
  - Restrição de compartilhamento no FileProvider seguindo as diretrizes de menor privilégio.

- **Estabilidade Geral:**
  - Otimizações no gerenciamento de arquivos e consistência em toda a biblioteca.
