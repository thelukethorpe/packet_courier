package thorpe.luke.network.simulator;

public class NodeState {
  private final String name;
  private final Mailbox mailbox;
  private final PostalService postalService;

  public NodeState(String name, Mailbox mailbox, PostalService postalService) {
    this.name = name;
    this.postalService = postalService;
    this.mailbox = mailbox;
  }

  public String getName() {
    return name;
  }

  public PostalService getPostalService() {
    return postalService;
  }

  public Mailbox getMailbox() {
    return mailbox;
  }
}
