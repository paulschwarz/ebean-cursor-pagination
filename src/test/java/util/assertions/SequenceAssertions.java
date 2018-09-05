package util.assertions;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;

import java.util.Iterator;
import java.util.List;
import java.util.function.Function;

public class SequenceAssertions {

  static public <T> void assertRange(Expectations<Integer> expectations, List<T> list, Function<T, Integer> get) {
    if (expectations.size() != list.size()) {
      fail(String.format("Different sizes, sequence has size %s and list has size %d",
          expectations.size(),
          list.size())
      );
    }

    int index = 0;
    for (Integer expected : expectations) {
      assertEquals(String.format("Unexpected value in sequence at index %d", index),
          expected, get.apply(list.get(index++)));
    }
  }

  static public Sequence sequence(Integer ...values) {
    return new Sequence(values);
  }

  static public Range range(int from, int to) {
    return new Range(from, to);
  }

  public interface Expectations<T> extends Iterable<T> {
    int size();
  }

  static public class Sequence implements Expectations<Integer> {

    Integer[] values;

    Sequence(Integer[] values) {
      this.values = values;
    }

    @Override
    public int size() {
      return values.length;
    }

    @Override
    public Iterator<Integer> iterator() {
      return new Iterator<Integer>() {
        private int index = 0;

        @Override
        public boolean hasNext() {
          return values.length > index;
        }

        @Override
        public Integer next() {
          return values[index++];
        }
      };
    }
  }

  static public class Range implements Expectations<Integer> {

    int from, to;

    Range(int from, int to) {
      this.from = from;
      this.to = to;
    }

    @Override
    public int size() {
      return Math.abs(to - from) + 1;
    }

    private boolean incrementing() {
      return to > from;
    }

    @Override
    public Iterator<Integer> iterator() {
      return new Iterator<Integer>() {
        private int index = 0;

        @Override
        public boolean hasNext() {
          return size() > index;
        }

        @Override
        public Integer next() {
          if (incrementing()) {
            return from + index++;
          } else {
            return from - index++;
          }
        }
      };
    }
  }
}
