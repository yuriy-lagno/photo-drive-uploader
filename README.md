# Photo Drive Uploader

Android-приложение: снимает фото и загружает его в Google Drive пользователя.

## Функционал
- Съёмка фото через камеру (CameraX)
- Превью снимка, подтверждение или пересъёмка
- Вход через Google-аккаунт (OAuth), загрузка фото в Google Drive
- Настройки: выбор/создание папки на Drive
- История загруженных фото со ссылками на файлы в Drive

## Сборка

APK собирается автоматически через GitHub Actions при пуше в ветку `main`/`master`
(workflow `.github/workflows/build-apk.yml`). Готовый файл появляется во вкладке
**Actions → (последний run) → Artifacts → photo-drive-uploader-debug-apk**.

Локально: `./gradlew assembleDebug` (нужны JDK 17 и Android SDK).

## Обязательная настройка перед использованием — Google Cloud Console

Без этого шага вход через Google и загрузка в Drive работать не будут.

1. Откройте https://console.cloud.google.com/ и создайте новый проект (или выберите существующий).
2. **Включите Google Drive API**: APIs & Services → Library → найдите "Google Drive API" → Enable.
3. **Настройте OAuth consent screen** (APIs & Services → OAuth consent screen):
   - User Type: External (если у вас обычный gmail-аккаунт).
   - Заполните название приложения, email.
   - На шаге Scopes добавьте `.../auth/drive.file`.
   - На шаге Test users добавьте свой email — пока приложение в статусе "Testing",
     входить смогут только добавленные тестовые пользователи.
4. **Создайте OAuth Client ID** (APIs & Services → Credentials → Create Credentials → OAuth client ID):
   - Application type: **Android**
   - Package name: `com.jural.photodriveuploader`
   - SHA-1 certificate fingerprint:
     ```
     4E:F9:88:7F:35:CF:33:38:19:F0:30:0B:3B:09:F1:E4:2D:D7:96:C1
     ```
     (это отпечаток debug-ключа `debug.keystore`, который в репозитории —
     им подписывается APK, собранный в GitHub Actions)
   - Сохраните.

После этого установите собранный APK на телефон (потребуется разрешить установку
из неизвестных источников) и войдите через свой Google-аккаунт в настройках приложения.

## Примечание про подпись

`debug.keystore` в репозитории — это фиксированный debug-ключ (не секретный,
стандартный пароль `android`), нужен только для того, чтобы SHA-1 не менялся
между сборками в CI. Для публикации в Google Play потребуется отдельный
release-ключ и обновление OAuth client ID под него.
