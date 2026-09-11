package task1;

public class Task1 {
    public static void main(String[] args) {
        if (args.length == 0 || args.length % 2 != 0) {
            System.err.println("Ожидается чётное число аргументов: n1 m1 [n2 m2 ...]");
            return;
        }

        StringBuilder result = new StringBuilder();

        // Обрабатываем все пары (n, m) — массивов может быть сколько угодно
        for (int i = 0; i < args.length; i += 2) {
            int n = Integer.parseInt(args[i]);
            int m = Integer.parseInt(args[i + 1]);
            result.append(buildPath(n, m));
        }

        System.out.println(result);
    }

    //Строит путь по круговому массиву 1..n с интервалом m. Путь — последовательность начал интервалов; цикл завершается,
    //когда следующее начало снова становится 1
    private static String buildPath(int n, int m) {
        StringBuilder sb = new StringBuilder();
        int cur = 1;

        do {
            sb.append(cur);
            // следующее начало = конец текущего интервала (шаг m-1 вперёд)
            cur = (cur - 1 + m - 1) % n + 1;
        } while (cur != 1);

        return sb.toString();
    }
}
