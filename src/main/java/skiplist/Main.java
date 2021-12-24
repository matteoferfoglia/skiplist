package skiplist;

/**
 * Class with examples of use of {@link SkipListMap}.
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
        SkipListMap<Integer, Integer> m = new SkipListMap<>();
        m.put(1, null);
        m.put(3, null);
        m.put(5, null);
        m.put(2, null);
        System.out.println(m);
        for (var a : m) {
            System.out.println(a);
        }

        SkipList<Integer> l = new SkipList<>();
        l.add(1);
        l.add(3);
        l.add(5);
        l.add(2);
        System.out.println(l);
        for (var a : l) {
            System.out.println(a);
        }
    }
}
