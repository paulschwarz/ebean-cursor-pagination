package util.factories;

import io.ebean.Ebean;
import me.paulschwarz.seeder.Factory;
import util.models.Example;

public class ExampleFactory extends Factory<Example> {

  private int nextRank = 100;
  private int parityValue = 0;

  public ExampleFactory() {
    super(model -> Ebean.getDefaultServer().insert(model));
  }

  @Override
  protected Example create() {
    // by dividing by 3 and truncating to an integer, we introduce duplicates
    int rank = nextRank-- / 3;
    String parity = parityValue++ % 2 == 0 ? "even" : "odd";

    return new Example(
        rank,
        parity
    );
  }
}
