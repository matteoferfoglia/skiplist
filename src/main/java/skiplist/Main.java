package skiplist;

/**
 * Class with examples of use of {@link SkipList}.
 *
 * @author Matteo Ferfoglia
 */
public class Main {

    /**
     * Main method.
     *
     * @param args Command-line args
     */
    public static void main(String[] args) {
        SkipList<Integer, Integer> l = new SkipList<>();
        l.put(1, null);
        l.put(3, null);
        l.put(5, null);
        l.put(2, null);
        System.out.println(l);

        for (var a : l) {
            System.out.println(a);
        }
    }
}
