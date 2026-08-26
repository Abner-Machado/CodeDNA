/** A tiny order-total program. Every method is a gene CodeDNA can knock out. */
public class Demo {

    static int[] parse(String order) {
        String[] field = order.split(",");
        return new int[]{Integer.parseInt(field[0]), Integer.parseInt(field[1])};
    }

    static boolean valid(int[] item) {
        return item[0] > 0 && item[1] > 0;
    }

    static int total(int[] item) {
        return item[0] * item[1];
    }

    static String format(int[] item, int total) {
        return String.format("%2d x %5d = %6d", item[0], item[1], total);
    }

    static void log(String message) {
        System.out.println("[log] " + message);
    }

    static void debug(String message) {
        System.out.println("[debug] " + message);
    }

    public static void main(String[] args) {
        debug("start");
        String[] orders = {"2,150", "1,899", "0,500", "5,20", "3,1200", "-1,10", "4,75"};
        int sum = 0;
        for (String order : orders) {
            int[] item = parse(order);
            if (!valid(item)) {
                log("skipped " + order);
                continue;
            }
            int price = total(item);
            sum += price;
            System.out.println(format(item, price));
        }
        System.out.println("TOTAL " + sum);
    }
}
