# Sorteio • B5 Soluções

Aplicativo Android nativo (Kotlin) para sorteios de rifa **sem repetir números**.

## Funcionalidades

- Defina a quantidade de números de 1 a 10.000 (sorteios de 1 a N).
- Número sorteado **enorme na tela**, com animação e vibração.
- Sorteio aleatório uniforme dos números disponíveis com `SecureRandom`.
- Números já sorteados **não participam** das próximas extrações.
- Persistência local do histórico mesmo após fechar ou reiniciar o aplicativo.
- Contadores de sorteados/restantes e barra de progresso.
- Histórico recente, consulta completa e compartilhamento dos resultados.
- Reiniciar sorteio com confirmação para impedir apagamentos acidentais.
- Funciona offline; não solicita conexão nem permissões invasivas.

## Download do APK

Acompanhe a publicação em [Releases](https://github.com/emanoelb5-lgtm/Sorteio/releases). A automação do GitHub Actions compila o APK a cada atualização da branch `main`.

> O APK v1.0.0 é uma versão de teste assinada automaticamente com a chave debug do Android. Para distribuir na Play Store, configure um keystore release privado e gere o AAB.

## Projeto

- Kotlin / Android Views (sem dependências de interface).
- Android 7.0+ (minSdk 24).
- Para compilar: JDK 17, Android SDK 35 e Gradle 8.10.2, `gradle :app:assembleDebug`.
- O histórico permanece somente no dispositivo e só é reiniciado por ação confirmada.

Desenvolvido para **B5 Soluções**.
