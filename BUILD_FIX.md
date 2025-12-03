# Решение проблемы сборки Maven

## Проблема
Maven блокирует HTTP репозитории и не может загрузить зависимости `javax.xml.bind:jaxb-api`, `com.sun.istack:istack-commons-runtime`, `com.sun.xml.fastinfoset:FastInfoset`.

## Решение 1: Использовать настройки Maven (рекомендуется)

Создайте файл `~/.m2/settings.xml` (или используйте `.mvn/settings.xml` в проекте):

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <mirrors>
        <mirror>
            <id>maven-default-http-blocker</id>
            <mirrorOf>external:http:*</mirrorOf>
            <name>Pseudo repository to mirror external repositories initially using HTTP.</name>
            <url>http://0.0.0.0/</url>
            <blocked>false</blocked>
        </mirror>
    </mirrors>
</settings>
```

Затем соберите проект:
```bash
mvn clean package -DskipTests
```

## Решение 2: Использовать системное свойство Maven

```bash
mvn clean package -DskipTests -Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true
```

## Решение 3: Временно отключить блокировку HTTP

Отредактируйте файл `~/.m2/settings.xml` и найдите секцию с `maven-default-http-blocker`, измените `<blocked>true</blocked>` на `<blocked>false</blocked>`.

## Решение 4: Использовать альтернативные репозитории

Добавьте в `pom.xml` репозитории с HTTPS:

```xml
<repositories>
    <repository>
        <id>central</id>
        <url>https://repo1.maven.org/maven2</url>
    </repository>
</repositories>
```

## Текущее состояние

В проекте уже:
- ✅ Добавлены исключения для проблемных зависимостей в MinIO
- ✅ Создан `.mvn/settings.xml` для разрешения HTTP
- ✅ Добавлен dependencyManagement с правильными версиями

Попробуйте собрать проект:
```bash
mvn clean package -DskipTests
```

Если не работает, используйте Решение 1.



