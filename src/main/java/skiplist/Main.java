package skiplist;

import java.io.*;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class with examples of use of {@link SkipListMap}.
 *
 * @author Matteo Ferfoglia
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class Main {

    /**
     * Main method.
     *
     * @param args Command-line args
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
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

        System.out.println("Update maxListLevel:\t" + m2.setMaxListLevel());

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

        System.out.println("Update maxListLevel:\t" + l3.setMaxListLevel());


        // map de/serialization
        File f = new File("exampleSerialization");
        if (f.exists()) {
            f.delete();
        }
        var veryLargeMap = new SkipListMap<Integer, String>();
        veryLargeMap.putAll(IntStream.range(0, 9999)
                .unordered().parallel()
                .mapToObj(i -> new AbstractMap.SimpleEntry<>(i, "FooBar"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        try (ObjectOutputStream oos = new ObjectOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(f, false)))) {
            oos.writeObject(veryLargeMap);
            oos.flush();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            //noinspection unchecked
            SkipListMap<Integer, String> veryLargeMapRead = (SkipListMap<Integer, String>) ois.readObject();
        }


        // list de/serialization
        if (f.exists()) {
            f.delete();
        }
        var veryLargeList = new SkipList<Integer>();
        veryLargeList.addAll(IntStream.range(0, 9999).boxed().collect(Collectors.toList()));

        try (ObjectOutputStream oosList = new ObjectOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream(f, false)))) {
            oosList.writeObject(veryLargeList);
            oosList.flush();
        }

        try (ObjectInputStream oisList = new ObjectInputStream(new BufferedInputStream(new FileInputStream(f)))) {
            //noinspection unchecked
            SkipList<Integer> veryLargeListRead = (SkipList<Integer>) oisList.readObject();
        }

        if (f.exists()) {
            f.delete();
        }
    }
}
