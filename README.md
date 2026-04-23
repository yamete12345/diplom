# Модуль управления банковскими счетами

Дипломный проект (Железнов Г. А., МУИВ, 2026) — клиентская часть модуля управления банковскими счетами: регистрация, вход с подтверждением по e-mail, открытие/закрытие счетов, пополнение, списание, перевод, история операций и экспорт в CSV.

## Стек
- Java 21 LTS (проект также собирается на Java 25)
- Spring Boot 3.3.5 (без web-слоя) — DI, `@Transactional`, Data JPA
- Swing + FlatLaf — UI
- H2 (file mode) — БД
- Flyway — миграции схемы
- BCrypt (`spring-security-crypto`) — хеширование паролей
- Jakarta Mail (через `spring-boot-starter-mail`) — отправка OTP

## Сборка и запуск

Требуется установленный JDK 21+ (`JAVA_HOME` должен указывать на корень JDK). Maven ставить не нужно — используется Maven Wrapper.

```cmd
:: Windows
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot
mvnw.cmd -DskipTests package
java -jar target\bank-accounts-module-1.0.0.jar
```

```bash
# Linux/macOS
export JAVA_HOME=/path/to/jdk
./mvnw -DskipTests package
java -jar target/bank-accounts-module-1.0.0.jar
```

База H2 создаётся в `./data/bankdb.mv.db` при первом запуске, Flyway применит миграцию V1 автоматически.

## Настройка отправки писем (OTP через Яндекс.Почту)

Режим реальной отправки **включён по умолчанию** (`app.mail.stub: false`). Письма уходят на тот e-mail, который пользователь ввёл при регистрации или входе.

SMTP настроен на `smtp.yandex.ru:465` (SSL). Логин/пароль берутся из переменных окружения, чтобы не коммитить секреты.

### Шаг 1. Получить пароль приложения Яндекса
1. Открой [id.yandex.ru](https://id.yandex.ru/) → **Безопасность** → **Пароли приложений**.
2. Нажми «Создать пароль», выбери тип «Почта», задай имя (например, «BankModule»).
3. Яндекс покажет 16-символьный пароль — это и есть `MAIL_PASSWORD`. Обычный пароль от аккаунта **не подходит**.

### Шаг 2. Задать переменные окружения и запустить

**Windows (cmd):**
```cmd
set MAIL_USERNAME=yourname@yandex.ru
set MAIL_PASSWORD=xxxxxxxxxxxxxxxx
java -jar target\bank-accounts-module-1.0.0.jar
```

**Windows (PowerShell):**
```powershell
$env:MAIL_USERNAME = "yourname@yandex.ru"
$env:MAIL_PASSWORD = "xxxxxxxxxxxxxxxx"
java -jar target\bank-accounts-module-1.0.0.jar
```

**Linux/macOS:**
```bash
export MAIL_USERNAME=yourname@yandex.ru
export MAIL_PASSWORD=xxxxxxxxxxxxxxxx
java -jar target/bank-accounts-module-1.0.0.jar
```

По желанию можно задать `MAIL_FROM` (адрес в поле «От кого»), иначе будет использоваться `MAIL_USERNAME`.

### Если нужно временно отключить отправку
Выстави `app.mail.stub: true` в [application.yml](src/main/resources/application.yml) — OTP будут печататься в лог вместо отправки.

## Функционал (клиент)

1. **Регистрация** — ФИО, e-mail, пароль (BCrypt). Подтверждение e-mail 6-значным OTP.
2. **Вход** — e-mail + пароль → OTP на почту → вход.
3. **Открытие счёта** — Текущий или Сберегательный, RUB. Генерируется 20-значный номер.
4. **Закрытие счёта** — только при нулевом балансе.
5. **Пополнение / Списание / Перевод** — с двойной записью через таблицу `postings` (DEBIT/CREDIT).
6. **История операций** — за 90 дней, фильтр по счёту; экспорт в CSV.
7. **Смена пароля** — проверка старого пароля + OTP.

## Структура проекта

```
src/main/java/ru/muiv/diplom/
├── Application.java              // точка входа: Spring Boot + Swing
├── config/SecurityBeans.java     // BCryptPasswordEncoder
├── domain/                        // JPA-сущности + enum'ы
├── repo/                          // Spring Data JPA репозитории
├── service/                       // Auth, Otp, Email, Account, Transaction
├── ui/                            // LoginFrame, RegisterFrame, OtpDialog,
│                                  // MainFrame, OperationDialog, ChangePasswordDialog
└── util/                          // Session, AccountNumberGenerator, CsvExporter

src/main/resources/
├── application.yml
└── db/migration/V1__init.sql     // схема БД
```

## Чек-лист ручного тестирования

1. Регистрация → OTP в логе / письме → подтверждение.
2. Повторный логин → OTP → главное окно.
3. Открытие двух счетов; оба номера уникальные.
4. Пополнение счёта №1 на 10 000 ₽; баланс обновился; операция в истории.
5. Перевод 3000 ₽ со счёта №1 на счёт №2 → балансы 7000 и 3000; в истории счёта №1 строка `DEBIT`, в истории счёта №2 — `CREDIT`.
6. Попытка списать больше баланса → ошибка, баланс не меняется.
7. Попытка закрыть счёт с ненулевым балансом → ошибка.
8. Закрытие пустого счёта → статус `CLOSED`.
9. Смена пароля: неверный OTP → отказ; верный → вход по новому паролю.
10. Экспорт истории в CSV → файл корректно открывается в Excel (BOM + UTF-8).
