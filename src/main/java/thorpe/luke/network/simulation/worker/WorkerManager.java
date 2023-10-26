package thorpe.luke.network.simulation.worker;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import thorpe.luke.log.BufferedFileLogger;
import thorpe.luke.log.Logger;
import thorpe.luke.network.packet.Packet;
import thorpe.luke.network.simulation.PacketCourierSimulation;
import thorpe.luke.network.simulation.mail.Mailbox;
import thorpe.luke.network.simulation.mail.PostalService;
import thorpe.luke.network.simulation.node.NodeTopology;
import thorpe.luke.time.Clock;
import thorpe.luke.util.error.ExceptionListener;

public class WorkerManager {

  private static final DateTimeFormatter CRASH_DUMP_DATE_FORMAT =
      DateTimeFormatter.ofPattern("yyyy_MM_dd_hh_mm_ss");

  private final WorkerAddress address;
  private final Clock clock;
  private final NodeTopology nodeTopology;
  private final Mailbox mailbox;
  private final PostalService postalService;
  private final Logger logger;
  private final ExceptionListener exceptionListener;
  private final Path crashDumpLocation;
  private final WorkerAddressGenerator workerAddressGenerator;
  private final WorkerAddressBook workerAddressBook;

  public WorkerManager(
      WorkerAddress address,
      Clock clock,
      NodeTopology nodeTopology,
      Mailbox mailbox,
      PostalService postalService,
      Logger logger,
      ExceptionListener exceptionListener,
      Path crashDumpLocation,
      WorkerAddressGenerator workerAddressGenerator,
      WorkerAddressBook workerAddressBook) {
    this.address = address;
    this.clock = clock;
    this.nodeTopology = nodeTopology;
    this.mailbox = mailbox;
    this.postalService = postalService;
    this.logger = logger;
    this.exceptionListener = exceptionListener;
    this.crashDumpLocation = crashDumpLocation;
    this.workerAddressGenerator = workerAddressGenerator;
    this.workerAddressBook = workerAddressBook;
  }

  public WorkerAddress getAddress() {
    return address;
  }

  public LocalDateTime getCurrentTime() {
    return clock.now();
  }

  public NodeTopology getTopology() {
    return nodeTopology;
  }

  public void sendMail(WorkerAddress destinationAddress, Packet packet) {
    postalService.mail(this.address, destinationAddress, packet);
  }

  public Packet waitForMail() {
    try {
      return mailbox.waitForMail();
    } catch (InterruptedException e) {
      throw new WorkerException(e);
    }
  }

  public Worker spawnChildWorker(WorkerScript workerScript) {
    WorkerAddress childWorkerAddress =
        workerAddressGenerator.generateUniqueChildWorkerAddress(address);
    return workerAddressBook.registerWorker(
        workerScript,
        childWorkerAddress,
        clock,
        nodeTopology,
        exceptionListener,
        crashDumpLocation,
        workerAddressGenerator,
        workerAddressBook,
        postalService,
        logger);
  }

  public void log(String message) {
    logger.log(message);
  }

  public boolean generateCrashDump(List<String> crashDumpContents) {
    if (crashDumpLocation == null) {
      return false;
    }
    LocalDateTime now = LocalDateTime.now();
    String crashDumpFileName =
        address.getHostingNodeAddress().getName().replaceAll("\\s+", "-")
            + "__"
            + CRASH_DUMP_DATE_FORMAT.format(now)
            + PacketCourierSimulation.CRASH_DUMP_FILE_EXTENSION;
    File crashDumpFile = crashDumpLocation.resolve(crashDumpFileName).toFile();
    BufferedFileLogger crashDumpFileLogger;
    try {
      crashDumpFileLogger = new BufferedFileLogger(crashDumpFile);
    } catch (IOException e) {
      exceptionListener.invoke(e);
      return false;
    }
    crashDumpContents.forEach(crashDumpFileLogger::log);
    crashDumpFileLogger.flush();
    crashDumpFileLogger.close();
    return true;
  }
}
