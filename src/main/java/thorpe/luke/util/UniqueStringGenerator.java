package thorpe.luke.util;

public class UniqueStringGenerator {
  private static final int INITIAL_ID = 0;
  private static final String PREFIX_UNIT = "$";
  private int nextId;
  private String prefix = PREFIX_UNIT;

  public UniqueStringGenerator() {
    this.nextId = INITIAL_ID;
  }

  public synchronized String generateUniqueString() {
    int id = nextId++;
    if (id < INITIAL_ID) {
      nextId = INITIAL_ID;
      prefix += PREFIX_UNIT;
      return generateUniqueString();
    }
    return prefix + id;
  }
}
