# Games Spoof Viewer

App Android mínimo que mostra, numa tela com cara de Ajustes, o mapa
**jogo → perfil** definido em `/product/etc/assets/games_spoof.prop` e os campos
`Build.*` que cada perfil aplica. Ele lê o arquivo com **exatamente** as mesmas
regras do `GamesPropsConfig` injetado no framework, então a tela reflete o que o
framework de fato aplica (campos comentados no `.prop` não aparecem).

- Sem ícone na gaveta de apps (não declara `LAUNCHER`).
- A entrada aparece na **página inicial do app Ajustes**, via injeção
  `com.android.settings.action.IA_SETTINGS` + categoria
  `com.android.settings.category.ia.homepage`.
- **Para a entrada no Ajustes aparecer, o APK precisa estar instalado como app de
  sistema** (ver abaixo). Como app de usuário comum a injeção é ignorada pelo
  Settings (mas a tela ainda pode ser aberta por um intent `VIEW`).
- Sem dependências externas (Activity pura, UI em código): build de CI simples.

## Compilar no GitHub (sem instalar nada)

1. Crie um repositório e suba estes arquivos (veja abaixo).
2. O workflow `.github/workflows/build.yml` roda sozinho a cada `push` (ou pelo
   botão **Run workflow**, aba *Actions* → *build-apk*).
3. Ao terminar, baixe o APK em **Actions → a execução → Artifacts →
   `games-spoof-viewer-debug`** (`app-debug.apk`).

```bash
cd games-spoof-viewer
git init && git add . && git commit -m "Games Spoof Viewer"
git branch -M main
git remote add origin git@github.com:SEU_USUARIO/SEU_REPO.git
git push -u origin main
```

## Instalar como app de sistema (para aparecer no Ajustes)

O APK é assinado com a chave de debug — serve; o que importa para a injeção é
estar na imagem de sistema, não a assinatura. Com root:

```
# coloque o APK em /product/app (ou /system_ext/app)
mkdir -p /product/app/GamesSpoofViewer
cp app-debug.apk /product/app/GamesSpoofViewer/GamesSpoofViewer.apk
chmod 644 /product/app/GamesSpoofViewer/GamesSpoofViewer.apk
# reinicie
```

Depois de reiniciar, a entrada **Games Spoof** aparece na página inicial do
Ajustes. (Também funciona empacotando como módulo Magisk que monte o APK nesse
caminho.)

## Observação importante sobre o seu `games_spoof.prop`

Pacotes com **letra maiúscula** no nome (ex.: `com.gameloft.android.ANMP.GloftA9HM`,
`com.miHoYo.GenshinImpact`, `com.levelinfinite.sgameGlobal`) só são spoofados se o
`apply()` do `GamesPropsConfig` comparar o nome do pacote em minúsculas — o
`load()` já normaliza a chave para minúsculas. Sem essa correção de uma linha no
framework, esses jogos não recebem o spoof. Este visualizador casa de forma
insensível a maiúsculas/minúsculas e exibe a grafia original.
