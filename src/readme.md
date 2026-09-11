# PerformanceTest

- Java 21 (моя версия)

Инструкция по запуску (на всякий случай, просто как я запускал на Windows)

### Task 1

Из директории `..\PerformanceTest\src`:

```
javac task1\Task1.java
java task1.Task1 5 3 4 2
(или любое другое количество пар чисел)
```

---

### Task 2

Из директории `..\PerformanceTest\src`:

```
javac task2\Task2.java
java task2.Task2 task2\ellipse.txt task2\points.txt
```

---

### Task 3

В задании не указано, как именно нужно его выполнить, поэтому реализовано два варианта: через собственный парсер (custom) и через подключение библиотеки Gson

Из директории `..\PerformanceTest`:

```
javac -cp "lib\gson-2.11.0.jar" -d out src\task3\Task3.java
```

**Запуск с ручным парсером:**

```
java -cp "out;lib\gson-2.11.0.jar" task3.Task3 src\task3\values.json src\task3\tests.json src\task3\report.json custom
```

**Запуск через библиотеку Gson (папка `lib`):**

```
java -cp "out;lib\gson-2.11.0.jar" task3.Task3 src\task3\values.json src\task3\tests.json src\task3\report.json gson
```

**Запуск по умолчанию:**

```
java -cp "out;lib\gson-2.11.0.jar" task3.Task3 src\task3\values.json src\task3\tests.json src\task3\report.json
```

По умолчанию используется Gson. Если библиотека недоступна — автоматически переключается на custom-парсер.

---

### Task 4

Из директории `..\PerformanceTest\src`:

```
javac task4\Task4.java
java task4.Task4 task4\nums1.txt
```