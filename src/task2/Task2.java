package task2;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Task2 {

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Должно быть 2 аргумента: <ellipseFile> <pointsFile>");
            return;
        }

        // Читаем эллипс
        List<String> ellipseLines = Files.readAllLines(Paths.get(args[0]));
        BigDecimal[] center = parsePair(ellipseLines.get(0));
        BigDecimal[] radii  = parsePair(ellipseLines.get(1));

        BigDecimal x0 = center[0];
        BigDecimal y0 = center[1];
        BigDecimal a  = radii[0];
        BigDecimal b  = radii[1];

        BigDecimal a2 = a.multiply(a);
        BigDecimal b2 = b.multiply(b);

        // rhs - правая часть уравнения
        BigDecimal rhs = a2.multiply(b2);

        // Обрабатываем точки
        StringBuilder out = new StringBuilder();
        for (String line : Files.readAllLines(Paths.get(args[1]))) {
            if (line.isBlank()) continue;

            BigDecimal[] p = parsePair(line);
            BigDecimal dx = p[0].subtract(x0);
            BigDecimal dy = p[1].subtract(y0);

            // lhs - левая часть уравнения
            BigDecimal lhs = dx.multiply(dx).multiply(b2).add(dy.multiply(dy).multiply(a2));

            // cmp - результат сравнения
            int cmp = lhs.compareTo(rhs);
            int result = (cmp == 0) ? 0 : (cmp < 0 ? 1 : 2);

            out.append(result).append(System.lineSeparator());
        }

        System.out.print(out);
    }

    // Разбираем строку вида "x y" в массив из двух BigDecimal
    private static BigDecimal[] parsePair(String line) {
        String[] parts = line.trim().split("\\s+");
        return new BigDecimal[] {
                new BigDecimal(parts[0]),
                new BigDecimal(parts[1])
        };
    }
}
