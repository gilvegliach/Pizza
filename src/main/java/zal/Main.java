package zal;

import java.util.*;
import java.util.concurrent.*;

// Run with:
// ./gradlew jar
// java -jar build/libs/proto-java.jar <inputs/example.in

public class Main {

  static final int[] SUBPIZZA_THRESHOLDS = new int[]{16, 24, 32, 54, 80};

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
    Res res = new Res();

    solution(res, r, c, l, h, pizza, es, t);
    for (Slice slice : res.slices) {
      flip(pizza, slice.r1, slice.c1, slice.r2, slice.c2);
    }
    long after = System.currentTimeMillis();
    long delta = (after - before) / 1000;

    System.err.println("Step: 1/2");
    System.err.println("Time: " + delta + " sec");
    System.err.println("Threshold: " + t);

    t *= 16;
    solution(res, r, c, l, h ,pizza, es, t);

    after = System.currentTimeMillis();
    delta = (after - before) / 1000;

    es.shutdownNow();

    int score = print(res);
    int size = r * c;
    System.err.println("---------------");
    System.err.println("Step: 2/2");
    System.err.println("Time: " + delta + " sec");
    System.err.println("Threshold: " + t);
    System.err.println("Score: " + score + "/" + size);
  }

  static void solution(Res result, int r, int c, int l, int h, int[][] pizza, ExecutorService es, int t) throws InterruptedException, ExecutionException {
    List<Slice> slices = new LinkedList<>();
    split(slices, t, new Slice(0, 0, r - 1, c - 1));

    List<Future<Res>> futureResults = new LinkedList<>();
    for (final Slice slice : slices) {
      Future<Res> futureResult = es.submit(() -> backtracking(l, h, slice, pizza));
      futureResults.add(futureResult);
    }

    for (Future<Res> futureResult : futureResults) {
      Res partialRes = futureResult.get();
      result.n += partialRes.n;
      result.slices.addAll(partialRes.slices);
      result.area += partialRes.area;
    }
  }


  static Res backtrackingRec(int l, int h, List<Slice> slices, int pos, int[][] pizza) {
    Res res = new Res();
    int n = slices.size();
    for (int i = pos; i < n; i++) {
      Slice slice = slices.get(i);
      if (isValid(l, h, pizza, slice.r1, slice.c1, slice.r2, slice.c2)) {
        flip(pizza, slice.r1, slice.c1, slice.r2, slice.c2);
        Res partialRes = backtrackingRec(l, h, slices, i + 1, pizza);
        int area = slice.area() * partialRes.area;
        if (res.area < area) {
          partialRes.area = area;
          partialRes.n++;
          partialRes.slices.add(0, slice);
          res = partialRes;
        }
        flip(pizza, slice.r1, slice.c1, slice.r2, slice.c2);
      }
    }
    return res;
  }

  static Res backtracking(int l, int h, Slice root, int[][] pizza) {
    List<Slice> slices = new LinkedList<>();
    for (int i = root.r1; i <= root.r2; i++) {
      for (int j = root.c1; j <= root.c2; j++) {
        if (pizza[i][j] < 0) continue;
        int pLimit = Math.min(root.r2, i + h);
        int qLimit = Math.min(root.c2, j + h);
        for (int p = i; p <= pLimit; p++) {
          for (int q = j; q <= qLimit; q++) {
            if (pizza[i][j] < 0) {
              qLimit = q - 1;
              break;
            }
            int area = area(i, j, p, q);
            if (area > h || area < 2 * l) continue;
            slices.add(new Slice(i, j, p, q));
          }
        }
      }
    }
    return backtrackingRec(l, h, slices, 0, pizza);
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