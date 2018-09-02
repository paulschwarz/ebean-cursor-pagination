package util.models;

import io.ebean.Model;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class Example extends Model {

  @Id
  private Integer id;
  private int rank;
  private String parity;

  public Example(int rank, String parity) {
    this.rank = rank;
    this.parity = parity;
  }

  public Integer getId() {
    return id;
  }

  public int getRank() {
    return rank;
  }

  public String getParity() {
    return parity;
  }

  @Override
  public String toString() {
    return String.format("%s(%d,%d,%s)",
        getClass().getSimpleName(),
        getId(),
        getRank(),
        getParity());
  }
}
