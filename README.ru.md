<div align="center">
  <img src="docs/images/Space-enl.png" alt="Space-Modernized" width="160" />
  <h1>Space-Modernized</h1>
</div>

***

## RU Версия 🇷🇺 | [ENG Version 🇺🇸](/README.md)

**Статус:** Бета (`0.1.0-beta`) \
**Версии Minecraft:** 1.20.1 (Forge) / 1.21.1 (NeoForge) \
**Mod ID:** `space_m` \
**Обязательные зависимости:** HBM-Modernized (`hbm_m`), Architectury API \
**Лицензия:** GPL-3.0-only (см. [Лицензия](#-лицензия))

***

## 📥 Официальные площадки

<div align="center">

# <img src="https://cdn-icons-png.flaticon.com/128/5968/5968756.png" height=28 /> <a href="https://discord.gg/f2BhvzG6CS">Discord</a> | <a href="../../releases">GitHub Releases</a> | <a href="../../issues">GitHub Issues</a>
</div>

> [!WARNING]
> **Мод находится на стадии активной бета-разработки.**
> **Делайте резервные копии миров перед установкой и обновлением!**
> Возможны баги, краши и изменения баланса/API.
> Сообщайте о проблемах в [GitHub Issues](../../issues)

***

## О моде

**Space-Modernized** — современная переработка известного аддона **NTM Space** для HBM's Nuclear Tech Mod, созданная эксклюзивно для **[HBM-Modernized](https://github.com/Raptor324/HBM-Modernized)** на Minecraft 1.20.1 (Forge) и 1.21.1 (NeoForge).

Исследуйте небесные тела, стройте ракеты и орбитальные станции, управляйте атмосферой и системами жизнеобеспечения, запускайте спутники и используйте космическую промышленную технику на современной кодовой базе и архитектуре рендеринга.

Проект является независимым портом и не является официальным релизом HBM/NTM или оригинального NTM Space.

***

## Исходники и авторство

Этот проект является модернизированным портом на основе **NTM Space** для Minecraft 1.7.10 и построен поверх **[HBM-Modernized](https://github.com/Raptor324/HBM-Modernized)** (созданного на базе **[Hbm's Nuclear Tech Mod](https://github.com/HbmMods/Hbm-s-Nuclear-Tech-GIT)**). Авторы и контрибьюторы оригинальных NTM Space и HBM/NTM сохраняют заслуги и копирайт на исходный код, ассеты и игровой дизайн.

Вся работа по модернизации — кодовая база 1.20.1/1.21.1, кросс-загрузчиковый слой и интеграция рендеринга — выполнена **FuchsDev**, **RaptorDev** и контрибьюторами (см. [gradle.properties](gradle.properties)).

***

## 📄 Лицензия

Проект распространяется под лицензией **[GPL-3.0-only](LICENSE)**.

- Поле `license` в метаданных мода указано как `GPL-3.0-only`.
- Файл `LICENSE` включается в релизные JAR-файлы в `META-INF/`.
- Распространяя скомпилированный JAR, вы обязаны предоставить полный соответствующий исходный код — включая скрипты сборки — способом, совместимым с GPLv3. Канонический репозиторий исходников — этот.

***

## 🏗️ Сборка

Проект использует единый исходный код с препроцессингом [Stonecutter](https://stonecutter.kikoz.dev/) для поддержки нескольких версий. Активные таргеты: `1.20.1-forge` (JDK 17) и `1.21.1-neoforge` (JDK 21).

```bash
./gradlew :1.20.1-forge:reobfJar     # собрать JAR для 1.20.1 Forge (versions/1.20.1-forge/build/libs/)
./gradlew :1.21.1-neoforge:jar       # собрать JAR для 1.21.1 NeoForge (versions/1.21.1-neoforge/build/libs/)
./gradlew :1.20.1-forge:runClient    # запустить дев-клиент 1.20.1 Forge
./gradlew :1.21.1-neoforge:runClient # запустить дев-клиент 1.21.1 NeoForge
./gradlew :1.20.1-forge:runData      # запустить датаген (только для Forge 1.20.1)
```

Перед коммитом убедитесь, что активный проект Stonecutter установлен на `1.20.1-forge` (значение по умолчанию в `stonecutter.gradle.kts`).

***

## 📦 Установка

1. Установите **Forge 1.20.1** или **NeoForge 1.21.1**
2. Установите **HBM-Modernized** (`hbm_m`) и **Architectury API** для вашей версии Minecraft
3. Скачайте актуальный `.jar` **Space-Modernized** со страницы [Releases](../../releases)
4. Положите все `.jar` файлы в папку `mods`

***

## 🤝 Участие в разработке

Pull request'ы, предложения и сообщения об ошибках приветствуются. Форкайте репозиторий и предлагайте улучшения, либо сообщайте о проблемах в [Issues](../../issues) с подробным описанием и логами.

***

## 💝 Благодарности

**Авторы и контрибьюторы оригинального NTM Space** — за создание космического аддона для 1.7.10

**The Bobcat** — автор оригинального HBM's Nuclear Tech Mod

**FuchsDev**, **RaptorDev / Raptor324** и контрибьюторы — модернизация и порт

Команды Forge, NeoForge, Architectury, Stonecutter и Mojang за инструменты разработки
