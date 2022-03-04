package thorpe.luke.network.packet;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import thorpe.luke.distribution.ExponentialDistribution;

public class SimulatedEventPipeline<Wrapper extends PacketWrapper<Wrapper>>
    implements PacketFilter<Wrapper> {

  private final List<State> states;
  private final StateTracker stateTracker;
  private final PriorityQueue<ScheduledEvent> eventQueue = new PriorityQueue<>();
  private final NeutralPacketFilter<Wrapper> outputBuffer;
  private final ChronoUnit timeUnit;
  private final Random random;
  private LocalDateTime now;

  public SimulatedEventPipeline(
      PacketPipeline<Wrapper> defaultPacketPipeline,
      List<NetworkEvent> networkEvents,
      ChronoUnit timeUnit,
      LocalDateTime startTime,
      Random random) {
    this.stateTracker = new StateTracker();
    this.outputBuffer = new NeutralPacketFilter<>();
    this.timeUnit = timeUnit;
    this.random = random;
    this.now = startTime;

    this.states = new ArrayList<>(networkEvents.size() + 1);
    State defaultState = new State(Integer.MIN_VALUE, 0.0, 0.0, defaultPacketPipeline);
    stateTracker.push(defaultState);
    states.add(defaultState);

    int nextPrecedence = 0;
    for (NetworkEvent networkEvent : networkEvents) {
      State state = new State(nextPrecedence++, networkEvent);
      states.add(state);
      LocalDateTime eventStartTime = sampleFromEventDurationDistribution(state.getMeanInterval());
      eventQueue.offer(new ScheduledEvent(eventStartTime, EventType.START, state));
    }
  }

  private LocalDateTime sampleFromEventDurationDistribution(double meanDuration) {
    ExponentialDistribution eventDurationDistribution =
        new ExponentialDistribution(1.0 / meanDuration);
    long eventDuration = Math.round(eventDurationDistribution.sample(random));
    return now.plus(Duration.of(eventDuration, timeUnit));
  }

  @Override
  public void tick(LocalDateTime now) {
    this.now = now;
    states
        .stream()
        .map(State::getPacketPipeline)
        .forEach(packetPipeline -> packetPipeline.tick(now));
    do {
      ScheduledEvent scheduledEvent = eventQueue.peek();
      if (scheduledEvent == null || scheduledEvent.getScheduledInvocationTime().isAfter(now)) {
        return;
      }
      eventQueue.poll();
      State state = scheduledEvent.getState();
      switch (scheduledEvent.getEventType()) {
        case START:
          stateTracker.push(state);
          LocalDateTime eventFinishTime =
              sampleFromEventDurationDistribution(state.getMeanDuration());
          eventQueue.offer(new ScheduledEvent(eventFinishTime, EventType.FINISH, state));
          break;
        case FINISH:
          stateTracker.pop(state);
          LocalDateTime eventStartTime =
              sampleFromEventDurationDistribution(state.getMeanInterval());
          eventQueue.offer(new ScheduledEvent(eventStartTime, EventType.START, state));
          break;
      }
    } while (true);
  }

  @Override
  public void enqueue(Wrapper packetWrapper) {
    stateTracker.getCurrentState().getPacketPipeline().enqueue(packetWrapper);
  }

  @Override
  public Optional<Wrapper> tryDequeue() {
    states
        .stream()
        .map(State::getPacketPipeline)
        .map(PacketPipeline::tryDequeue)
        .forEach(packetWrapper -> packetWrapper.ifPresent(outputBuffer::enqueue));
    return outputBuffer.tryDequeue();
  }

  private class State implements Comparable<State> {
    private final int precedence;
    private final double meanInterval;
    private final double meanDuration;
    private final PacketPipeline<Wrapper> packetPipeline;

    private State(
        int precedence,
        double meanInterval,
        double meanDuration,
        PacketPipeline<Wrapper> packetPipeline) {
      this.precedence = precedence;
      this.meanInterval = meanInterval;
      this.meanDuration = meanDuration;
      this.packetPipeline = packetPipeline;
    }

    private State(int precedence, NetworkEvent networkEvent) {
      this(
          precedence,
          networkEvent.getMeanInterval(),
          networkEvent.getMeanDuration(),
          new PacketPipeline<>(networkEvent.getPacketPipelineParameters(), now));
    }

    public double getMeanInterval() {
      return meanInterval;
    }

    public double getMeanDuration() {
      return meanDuration;
    }

    public PacketPipeline<Wrapper> getPacketPipeline() {
      return packetPipeline;
    }

    @Override
    public int compareTo(State that) {
      return Integer.compare(this.precedence, that.precedence);
    }
  }

  private class ScheduledEvent implements Comparable<ScheduledEvent> {

    private final LocalDateTime scheduledInvocationTime;
    private final EventType eventType;
    private final State state;

    private ScheduledEvent(
        LocalDateTime scheduledInvocationTime, EventType eventType, State state) {
      this.scheduledInvocationTime = scheduledInvocationTime;
      this.eventType = eventType;
      this.state = state;
    }

    public LocalDateTime getScheduledInvocationTime() {
      return scheduledInvocationTime;
    }

    public EventType getEventType() {
      return eventType;
    }

    public State getState() {
      return state;
    }

    @Override
    public int compareTo(ScheduledEvent that) {
      return this.scheduledInvocationTime.compareTo(that.scheduledInvocationTime);
    }
  }

  private enum EventType {
    START,
    FINISH
  }

  private class StateTracker {
    private final TreeSet<State> states;

    private StateTracker() {
      this.states = new TreeSet<>();
    }

    public State getCurrentState() {
      return states.last();
    }

    public void push(State state) {
      states.add(state);
    }

    public void pop(State state) {
      states.remove(state);
    }
  }
}
