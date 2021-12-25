package skiplist;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Generalization of {@link java.util.function.BiFunction} to take
 * three input arguments and produce one output argument.
 * From <a href="https://stackoverflow.com/a/19649473">Stackoverflow</a>.
 *
 * @param <A> Input type for argument 1.
 * @param <B> Input type for argument 2.
 * @param <C> Input type for argument 3.
 * @param <R> Output type.
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {

    R apply(A a, B b, C c);

    default <V> TriFunction<A, B, C, V> andThen(
            Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (A a, B b, C c) -> after.apply(apply(a, b, c));
    }

    default <D, V> TriFunction<A, B, C, V> andThen(
            BiFunction<? super R, D, ? extends V> after, D parameter) {
        Objects.requireNonNull(after);
        return (A a, B b, C c) -> after.apply(apply(a, b, c), parameter);
    }
}
