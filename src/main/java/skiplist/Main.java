package skiplist;

import java.util.Arrays;

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
        System.out.println("m:\t" + m);
        for (var a : m) {
            System.out.println(a);
        }

        SkipListMap<Integer, Integer> m2 = new SkipListMap<>();
        m2.putAll(m);
        System.out.println("m2:\t" + m2);

        SkipList<Integer> l1 = new SkipList<>();
        l1.add(1);
        l1.add(3);
        l1.add(5);
        l1.add(2);
        System.out.println("l1:\t" + l1);
        for (var a : l1) {
            System.out.println(a);
        }

        SkipList<Integer> l2 = new SkipList<>();
        l2.add(3);
        l2.add(4);
        l2.add(5);
        System.out.println("l2:\t" + l2);

        System.out.println("union:\t" + SkipList.union(l1, l2));
        System.out.println("intersection:\t" + SkipList.intersection(l1, l2));

        SkipList<Integer> l3 = new SkipList<>();
        l3.addAll(Arrays.asList(1, 5, 3, 4, 6));
        System.out.println("l3:\t" + l3);
    }
}
