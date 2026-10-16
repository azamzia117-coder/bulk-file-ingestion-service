import java.time.LocalDate;
import java.util.Random;

/**
 * Writes a synthetic transaction CSV (no real accounts). About 1% of rows are deliberately invalid.
 * Usage: java tools/GenerateTransactions.java 100000 > sample-data/transactions.csv
 */
public class GenerateTransactions {
    public static void main(String[] args) {
        int rows = args.length > 0 ? Integer.parseInt(args[0]) : 1000;
        Random rnd = new Random(7);
        System.out.println("record_id,account_id,amount_minor,currency,posted_date");
        LocalDate start = LocalDate.of(2026, 1, 1);
        for (int i = 1; i <= rows; i++) {
            boolean bad = rnd.nextDouble() < 0.01;
            String account = bad ? "ACC-12" : String.format("ACC-%06d", rnd.nextInt(999_999));
            long amount = (rnd.nextBoolean() ? 1 : -1) * (100 + rnd.nextInt(500_000));
            String date = start.plusDays(rnd.nextInt(180)).toString();
            System.out.printf("T%08d,%s,%d,USD,%s%n", i, account, amount, date);
        }
    }
}
