# NERF Music

Aplicativo Android de música em Kotlin, com interface construída em Jetpack Compose e reprodução por AndroidX Media3. O app reúne busca e navegação por um catálogo musical, player em segundo plano, biblioteca local e recursos de personalização.

> O catálogo e a resolução de streams dependem de serviços e endpoints externos do YouTube Music. O funcionamento dessas integrações pode mudar quando os serviços alterarem seus endpoints ou formatos.

## Recursos

- Busca de músicas e artistas, navegação por artistas e álbuns e feed inicial com seções e filtros de humor.
- Player com fila, controles na notificação e reprodução em segundo plano; configurações de reprodução incluem gapless, crossfade e automix.
- Biblioteca local com músicas curtidas, playlists, histórico, downloads para reprodução offline e letras.
- Feed e rádio sintetizados com base em histórico, artistas e afinidade musical.
- Downloads inteligentes em segundo plano, com limites e opções de qualidade configuráveis.
- Configurações de rede, reprodução, conteúdo, armazenamento e exibição.
- Ferramentas de migração/importação de biblioteca e verificação de atualizações do app.

Os recursos que dependem da rede exigem conexão à internet. A disponibilidade de faixas, letras, metadados e streams depende das fontes externas usadas pelo aplicativo.

## Tecnologias

- Kotlin e Android Gradle Plugin, com Gradle Wrapper.
- Jetpack Compose, Material 3 e Navigation Compose.
- AndroidX Media3 (ExoPlayer e MediaSession) para reprodução em primeiro e segundo plano.
- OkHttp e Kotlin Serialization para chamadas de rede e dados serializados.
- NewPipe Extractor e resolvedores InnerTube para localizar streams de áudio.
- WorkManager para tarefas periódicas, como downloads inteligentes e verificações de atualização.

## Organização do código

O código-fonte do aplicativo fica em `app/src/main/java/com/nerf/music/`:

| Pacote | Responsabilidade |
| --- | --- |
| `data/model` | Modelos de faixas, artistas, álbuns, feed, biblioteca e estado de reprodução. |
| `data/network` | Cliente do catálogo e interpretação das respostas do InnerTube. |
| `data/repository` | Acesso ao catálogo e persistência local de biblioteca, histórico, preferências, afinidade, downloads e migração. |
| `player` | Serviço Media3, resolução de streams, rádio e lógica de transição. |
| `ui/screens` | Telas de Home, busca, exploração, player, biblioteca, artistas, álbuns, configurações e migração. |
| `ui/viewmodel` | Estado e operações que conectam a interface aos repositórios e ao player. |
| `updater`, `work`, `util` | Atualizações, tarefas de fundo, notificações e utilitários de armazenamento e rede. |

## Requisitos

- JDK 17
- Android SDK com plataforma 34
- Gradle Wrapper incluído no repositório (`gradlew` / `gradlew.bat`)

## Compilar e instalar

No Windows, a partir da raiz do projeto:

```powershell
.\gradlew.bat assembleDebug
```

O APK de depuração será gerado em `app\build\outputs\apk\debug\app-debug.apk`. Para instalar em um dispositivo conectado com ADB:

```powershell
.\gradlew.bat installDebug
```

Para compilar a variante de lançamento:

```powershell
.\gradlew.bat assembleRelease
```

As configurações de assinatura da variante de lançamento estão no `app/build.gradle.kts` e usam `app/nerf-release.jks`, com suporte a variáveis de ambiente para as senhas e o alias. Configure credenciais próprias antes de gerar uma distribuição assinada; não publique credenciais no repositório.

## Testes

Os testes unitários ficam em `app/src/test/`. Para executá-los:

```powershell
.\gradlew.bat test
```

## Versão

A versão Android está definida em `app/build.gradle.kts` (`versionName` e `versionCode`). O histórico de alterações está em [CHANGELOG.md](CHANGELOG.md), e as notas da versão atual estão em [RELEASE_NOTES.md](RELEASE_NOTES.md).
