package zal;

import java.util.*;
import java.util.concurrent.*;

// Run with:
// ./gradlew jar
// java -jar build/libs/proto-java.jar <inputs/example.in

public class Main {

  static final int[] SUBPIZZA_THRESHOLDS = new int[]{16, 24, 32, 54, 58};

  public static void main(String[] args) throws Exception {
    Scanner sc = new Scanner(System.in);
    int r = sc.nextInt();
    int c = sc.nextInt();
    int l = sc.nextInt();
    int h = sc.nextInt();
    sc.nextLine();
    int[][] pizza = new int[r][c];
    for (int i = 0; i < r; i++) {
      String s = sc.nextLine();
      for (int j = 0; j < c; j++) {
        // T is 1, M is 2
        pizza[i][j] = s.charAt(j) == 'T' ? 1 : 2;
      }
    }

    ExecutorService es = Executors.newFixedThreadPool(3);

    long before = System.currentTimeMillis();
    int t = threshold((float) l / h);
    List<Slice> slices = new ArrayList<>();
    split(slices, t, new Slice(0, 0, r - 1, c - 1));

    List<Future<Res>> futureResults = new ArrayList<>();
    for (final Slice slice : slices) {
      Future<Res> futureResult = es.submit(() -> solution(l, h, slice, pizza));
      futureResults.add(futureResult);
    }

    Res res = new Res();
    for (Future<Res> futureResult : futureResults) {
      Res partialRes = futureResult.get();
      res.n += partialRes.n;
      res.slices.addAll(partialRes.slices);
      res.area += partialRes.area;
    }
    long after = System.currentTimeMillis();
    es.shutdownNow();

    int score = print(res);
    long delta = (after - before) / 1000;
    int size = r * c;
    System.err.println("---------------");
    System.err.println("Time: " + delta + "sec");
    System.err.println("Threshold: " + t);
    System.err.println("Score: " + score + "/" + size);
  }

  static Res solution(int l, int h, Slice slice, int[][] pizza) {
    Res res = new Res();
    for (int i = slice.r1; i <= slice.r2; i++) {
      for (int j = slice.c1; j <= slice.c2; j++) {
        if (pizza[i][j] <= 0) continue;
        for (int p = i; p <= slice.r2; p++) {
          for (int q = j; q <= slice.c2; q++) {
            if (isValid(l, h, pizza, i, j, p, q)) {
              flip(pizza, i, j, p, q);
              Res partialRes = solution(l, h, slice, pizza);
              int area = area(i, j, p, q) * partialRes.area;
              if (res.area < area) {
                partialRes.area = area;
                partialRes.n++;
                partialRes.slices.add(0, new Slice(i, j, p, q));
                res = partialRes;
              }
              flip(pizza, i, j, p, q);
            }
          }
        }
      }
    }
    return res;
  }

  static int print(Res res) {
    System.out.println(res.n);
    int score = 0;
    for (int i = 0; i < res.n; i++) {
      Slice slice = res.slices.get(i);
      System.out.println(slice);
      score += slice.area();
    }
    return score;
  }

  static int threshold(float r) {
    int limit = (int) (r * 10.f);
    return SUBPIZZA_THRESHOLDS[limit];
  }

  static void split(List<Slice> slices, int threshold, Slice root) {
    if (root.area() <= threshold) {
      slices.add(root);
      return;
    }
    int r = root.r2 - root.r1 + 1;
    int c = root.c2 - root.c1 + 1;
    if (r > c) {
      int mr = root.r1 + r / 2;
      split(slices, threshold, new Slice(root.r1, root.c1, mr, root.c2));
      split(slices, threshold, new Slice(mr + 1, root.c1, root.r2, root.c2));
    } else {
      int mc = root.c1 + c / 2;
      split(slices, threshold, new Slice(root.r1, root.c1, root.r2, mc));
      split(slices, threshold, new Slice(root.r1, mc + 1, root.r2, root.c2));
    }
  }

  static void flip(int[][] pizza, int i, int j, int p, int q) {
    for (int ii = i; ii <= p; ii++)
      for (int jj = j; jj <= q; jj++)
        pizza[ii][jj] = -pizza[ii][jj];
  }

  static boolean isValid(int l, int h, int[][] pizza, int i, int j, int p, int q) {
    int n = (p - i + 1) * (q - j + 1);
    if (n > h || n < 2 * l) return false;
    int t = 0;
    int m = 0;
    for (int ii = i; ii <= p; ii++) {
      for (int jj = j; jj <= q; jj++) {
        int cell = pizza[ii][jj];
        if (cell < 0) return false;
        if (cell == 1) t++;
        if (cell == 2) m++;
      }
    }
    return t >= l && m >= l;
  }

  static int area(int r1, int c1, int r2, int c2) {
    return (r2 - r1 + 1) * (c2 - c1 + 1);
  }

  static class Slice {
    int r1;
    int c1;
    int r2;
    int c2;

    Slice(int r1, int c1, int r2, int c2) {
      this.r1 = r1;
      this.c1 = c1;
      this.r2 = r2;
      this.c2 = c2;
    }

    int area() {
      return Main.area(r1, c1, r2, c2);
    }

    @Override
    public String toString() {
      return String.format("%d %d %d %d", r1, c1, r2, c2);
    }

  }

  static class Res {
    int n = 0;
    int area = 1;
    List<Slice> slices = new LinkedList<>();
  }
}