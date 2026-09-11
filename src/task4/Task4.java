package task4;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Task4 {

    private static final int MAX_MOVES = 20;

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Должен быть аргумент <numsFile>");
            return;
        }

        List<Integer> nums = readNumbers(args[0]);

        if (nums.isEmpty()) {
            System.err.println("Файл не содержит чисел");
            return;
        }

        int moves = minMoves(nums);

        if (moves > MAX_MOVES) {
            System.out.println(MAX_MOVES + " ходов недостаточно для приведения всех элементов массива к одному числу");
        } else {
            System.out.println(moves);
        }
    }

    // Читаем все числа из файла, по одному на строку
    private static List<Integer> readNumbers(String path) throws IOException {
        List<Integer> nums = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(path))) {
            String s = line.trim();
            if (s.isEmpty()) continue;
            nums.add(Integer.parseInt(s));
        }
        return nums;
    }


    // Минимум суммы |nums[i] - target| достигается при target = медиана
    // Для чётного размера подходит любой из двух центральных элементов
    private static int minMoves(List<Integer> nums) {
        List<Integer> sorted = new ArrayList<>(nums);
        Collections.sort(sorted);

        int median = sorted.get(sorted.size() / 2);

        int total = 0;
        for (int x : sorted) {
            total += Math.abs(x - median);
        }
        return total;
    }
}
