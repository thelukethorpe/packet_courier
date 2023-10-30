package thorpe.luke.network.simulation;

import thorpe.luke.time.Tickable;

public interface Simulation extends Tickable {
    boolean isComplete();
}
