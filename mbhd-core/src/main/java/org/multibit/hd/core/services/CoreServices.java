package org.multibit.hd.core.services;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bitcoinj.utils.Threading;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.multibit.hd.brit.crypto.PGPUtils;
import org.multibit.hd.brit.seed_phrase.Bip39SeedPhraseGenerator;
import org.multibit.hd.brit.seed_phrase.SeedPhraseGenerator;
import org.multibit.hd.brit.services.FeeService;
import org.multibit.hd.core.concurrent.SafeExecutors;
import org.multibit.hd.core.config.BitcoinConfiguration;
import org.multibit.hd.core.config.Configuration;
import org.multibit.hd.core.config.Configurations;
import org.multibit.hd.core.config.Yaml;
import org.multibit.hd.core.dto.HistoryEntry;
import org.multibit.hd.core.dto.WalletId;
import org.multibit.hd.core.dto.WalletPassword;
import org.multibit.hd.core.dto.WalletSummary;
import org.multibit.hd.core.events.CoreEvents;
import org.multibit.hd.core.events.ShutdownEvent;
import org.multibit.hd.core.exceptions.CoreException;
import org.multibit.hd.core.logging.LoggingFactory;
import org.multibit.hd.core.managers.InstallationManager;
import org.multibit.hd.core.managers.WalletManager;
import org.multibit.hd.core.utils.BitcoinNetwork;
import org.multibit.hd.hardware.core.HardwareWalletClient;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.hardware.core.wallets.HardwareWallets;
import org.multibit.hd.hardware.trezor.clients.TrezorHardwareWalletClient;
import org.multibit.hd.hardware.trezor.wallets.v1.TrezorV1HidHardwareWallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * <p>Factory to provide the following to application API:</p>
 * <ul>
 * <li>Entry point to configured instances of Core services</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class CoreServices {

  private static final Logger log = LoggerFactory.getLogger(CoreServices.class);

  /**
   * The URL of the live matcher daemon
   */
  public static final String LIVE_MATCHER_URL = "http://localhost:9090/brit";

  // TODO these should point to the multibit.org with the real matcher key
  /**
   * The live matcher PGP public key file
   */
  public static final String LIVE_MATCHER_PUBLIC_KEY_FILE = "multibit-org-matcher-key.asc";

  /**
   * Keep track of selected application events (e.g. exchange rate changes, security alerts etc)
   * Not an optional service
   */
  private static ApplicationEventService applicationEventService;

  /**
   * Keep track of security events (e.g. debugger, file permissions etc) across all wallets
   * Not an optional service
   */
  private static SecurityCheckingService securityCheckingService;

  /**
   * Keep track of shutdown events and ensure the configuration is persisted
   * Not an optional service
   */
  private static ConfigurationService configurationService;

  /**
   * Keep track of the Bitcoin network for the current wallet
   * Optional service until wallet is unlocked
   */
  private static Optional<BitcoinNetworkService> bitcoinNetworkService = Optional.absent();

  /**
   * Keep track of the hardware wallet service for the application
   * Optional service if system does not support hardware wallets (or none attached)
   */
  private static Optional<HardwareWalletService> hardwareWalletService = Optional.absent();

  /**
   * Keeps track of the contact service for the current wallet
   * Optional service until wallet is unlocked
   */
  private static Optional<PersistentContactService> contactService = Optional.absent();

  /**
   * Keeps track of the wallet service for the current wallet
   * Optional service until wallet is unlocked
   */
  private static Optional<WalletService> walletService = Optional.absent();

  /**
   * Keeps track of the history service for the current wallet
   * Optional service until wallet is unlocked
   */
  private static Optional<PersistentHistoryService> historyService = Optional.absent();

  /**
   * Keeps track of the backup service for the current wallet
   * Optional service until wallet is unlocked
   */
  private static Optional<BackupService> backupService = Optional.absent();

  /**
   * Manages CoreService startup and shutdown operations
   */
  private static ListeningExecutorService coreServices = SafeExecutors.newFixedThreadPool(10, "core-services");

  /**
   * Utilities have a private constructor
   */
  private CoreServices() {
  }

  /**
   * <p>Initialises the core services, and can act as an independent starting point for headless operations</p>
   *
   * @param args Any command line arguments
   */
  public static void main(String[] args) {

    // Order is important here
    applicationEventService = new ApplicationEventService();
    securityCheckingService = new SecurityCheckingService();
    configurationService = new ConfigurationService();

    // Start the logging factory (see later for instance)
    LoggingFactory.bootstrap();

    // Start the configuration service to ensure shutdown events are trapped
    configurationService.start();

    log.debug("Loading configuration...");
    Optional<Configuration> configuration;
    try (InputStream is = new FileInputStream(InstallationManager.getConfigurationFile())) {
      // Load configuration (providing a default if none exists)
      configuration = Yaml.readYaml(is, Configuration.class);
    } catch (IOException e) {
      configuration = Optional.absent();
    }

    if (configuration.isPresent()) {
      log.info("Using current configuration");
      Configurations.currentConfiguration = configuration.get();
    } else {
      log.warn("Using default configuration");
      Configurations.currentConfiguration = Configurations.newDefaultConfiguration();
    }

    // Configure logging now that we have a configuration
    new LoggingFactory(Configurations.currentConfiguration.getLogging(), "DigiBit HD").configure();

    // Start security checking service
    securityCheckingService.start();

    // Start application event service
    applicationEventService.start();

    // Configure Bitcoinj
    Threading.UserThread.WARNING_THRESHOLD = Integer.MAX_VALUE;

  }

  /**
   * <p>Typically called directly after a ShutdownEvent is broadcast.</p>
   * <p>Depending on the shutdown type this method will trigger a <code>System.exit(0)</code> to ensure graceful termination.</p></p>
   *
   * @param shutdownType The
   */
  @SuppressFBWarnings({"DM_GC"})
  public static synchronized void shutdownNow(final ShutdownEvent.ShutdownType shutdownType) {

    switch (shutdownType) {
      case HARD:
        log.info("Applying hard shutdown.");

        shutdownWalletSupportServices(shutdownType);
        shutdownApplicationSupportServices(shutdownType);

        log.info("Issuing system exit");
        System.exit(0);
        break;
      case SOFT:
        log.info("Applying soft shutdown.");

        shutdownWalletSupportServices(shutdownType);
        shutdownApplicationSupportServices(shutdownType);

        // Suggest a garbage collection to free resources under test
        System.gc();
        break;
      case SWITCH:
        log.info("Applying wallet switch.");

        shutdownWalletSupportServices(shutdownType);

        // Suggest a garbage collection
        System.gc();
        break;
    }

  }

  /**
   * <p>Shutdown all application support services (non-optional)</p>
   * <ul>
   * <li>Contact service</li>
   * <li>History service</li>
   * <li>Bitcoin network service</li>
   * <li>Wallet service</li>
   * <li>Backup service</li>
   * </ul>
   *
   * @param shutdownType The shutdown type providing context
   */
  private static void shutdownWalletSupportServices(ShutdownEvent.ShutdownType shutdownType) {

    // Allow graceful shutdown of managed services in the correct order
    shutdownService(contactService, shutdownType);
    shutdownService(historyService, shutdownType);

    // Close the Bitcoin network service (peer group, save wallet etc)
    shutdownService(bitcoinNetworkService, shutdownType);
    shutdownService(walletService, shutdownType);
    shutdownService(backupService, shutdownType);

    // Clear the references
    bitcoinNetworkService = Optional.absent();
    contactService = Optional.absent();
    walletService = Optional.absent();
    historyService = Optional.absent();
    backupService = Optional.absent();

  }

  /**
   * <p>Shutdown all application support services (non-optional)</p>
   * <ul>
   * <li>Hardware wallet service</li>
   * </ul>
   *
   * @param shutdownType The shutdown type providing context
   */
  private static void shutdownApplicationSupportServices(ShutdownEvent.ShutdownType shutdownType) {

    // Shutdown non-managed services
    if (hardwareWalletService.isPresent()) {
      hardwareWalletService.get().stopAndWait();
      // Need a fresh instance for correct restart
      hardwareWalletService = Optional.absent();
    }

    // Allow graceful shutdown in the correct order
    if (configurationService != null) {
      configurationService.shutdownNow(shutdownType);
    }
    if (securityCheckingService != null) {
      securityCheckingService.shutdownNow(shutdownType);
    }

    if (applicationEventService != null) {
      applicationEventService.shutdownNow(shutdownType);
    }

    // Be judicious when clearing references since it leads to complex behaviour during shutdown

  }

  /**
   * <p>Shutdown a managed service</p>
   *
   * @param service      The service
   * @param shutdownType The shutdown type providing context
   */
  private static void shutdownService(Optional<? extends ManagedService> service, ShutdownEvent.ShutdownType shutdownType) {
    if (service.isPresent()) {
      service.get().shutdownNow(shutdownType);
    }
  }

  /**
   * @param bitcoinConfiguration The Bitcoin configuration providing exchange and currency details
   *
   * @return A new exchange service based on the current configuration
   */
  public static ExchangeTickerService createAndStartExchangeService(BitcoinConfiguration bitcoinConfiguration) {

    // Breaks the "get or create" pattern because it is used to examine all exchanges

    log.debug("Create and start new exchange ticker service");
    final ExchangeTickerService exchangeTickerService = new ExchangeTickerService(bitcoinConfiguration);
    exchangeTickerService.start();

    return exchangeTickerService;

  }

  /**
   * @return Create a new hardware wallet service or return the extant one
   */
  public static synchronized Optional<HardwareWalletService> getOrCreateHardwareWalletService() {

    log.debug("Get hardware wallet service");
    if (!hardwareWalletService.isPresent()) {

      // Attempt Trezor support
      if (Configurations.currentConfiguration.isTrezor()) {

        try {
          // Use factory to statically bind a specific hardware wallet
          TrezorV1HidHardwareWallet wallet = HardwareWallets.newUsbInstance(
            TrezorV1HidHardwareWallet.class,
            Optional.<Integer>absent(),
            Optional.<Integer>absent(),
            Optional.<String>absent()
          );
          // Wrap the hardware wallet in a suitable client to simplify message API
          HardwareWalletClient client = new TrezorHardwareWalletClient(wallet);

          // Wrap the client in a service for high level API suitable for downstream applications
          hardwareWalletService = Optional.of(new HardwareWalletService(client));

        } catch (Throwable throwable) {
          log.warn("Could not create the hardware wallet.", throwable);
          hardwareWalletService = Optional.absent();
        }
      } else {
        hardwareWalletService = Optional.absent();
      }

    }

    return hardwareWalletService;

  }

  /**
   * Simplify FEST testing for hardware wallets
   *
   * @param service The hardware wallet service to use
   */
  public static void setHardwareWalletService(HardwareWalletService service) {
    Preconditions.checkState(InstallationManager.unrestricted, "The hardware wallet service should only be set in the context of testing");
    hardwareWalletService = Optional.of(service);
  }

  /**
   * <p>Stop the hardware wallet service</p>
   */
  public static void stopHardwareWalletService() {

    log.debug("Stop hardware wallet service (expect all subscribers to be purged)");
    hardwareWalletService.get().stopAndWait();

    // Clear the reference to avoid restart issues
    hardwareWalletService = Optional.absent();

  }

  /**
   * @return Create a new backup service or return the extant one
   */
  public static BackupService getOrCreateBackupService() {

    log.debug("Getting backup service");
    if (!backupService.isPresent()) {
      backupService = Optional.of(new BackupService());
    }

    return backupService.get();
  }

  /**
   * @return Create a new seed phrase generator
   */
  public static SeedPhraseGenerator newSeedPhraseGenerator() {

    log.debug("Creating new BIP39 seed phrase generator");
    return new Bip39SeedPhraseGenerator();
  }

  /**
   * @return The application event service singleton
   */
  public static ApplicationEventService getApplicationEventService() {
    return applicationEventService;

  }

  /**
   * @return The security checking service singleton
   */
  public static SecurityCheckingService getSecurityCheckingService() {
    log.debug("Get security checking service");
    return securityCheckingService;
  }

  /**
   * @return The configuration service singleton
   */
  public static ConfigurationService getConfigurationService() {
    log.debug("Get configuration service");
    return configurationService;
  }

  /**
   * @return The Bitcoin network service - note that this is NOT started
   */
  public static synchronized BitcoinNetworkService getOrCreateBitcoinNetworkService() {
    log.trace("Get Bitcoin network service ");

    // Require session singleton so only a null will create a new instance
    if (!bitcoinNetworkService.isPresent()) {
      bitcoinNetworkService = Optional.of(new BitcoinNetworkService(BitcoinNetwork.current().get()));
    }

    return bitcoinNetworkService.get();

  }

  /**
   * <p>Stop the Bitcoin network service and allow garbage collection</p>
   *
   * <p>This occurs on the CoreServices task thread</p>
   */
  public static synchronized void stopBitcoinNetworkService() {

    log.debug("Stop Bitcoin network service");
    coreServices.submit(
      new Runnable() {
        @Override
        public void run() {
          if (bitcoinNetworkService.isPresent()) {
            bitcoinNetworkService.get().shutdownNow(ShutdownEvent.ShutdownType.HARD);
            bitcoinNetworkService = Optional.absent();
          }
        }
      });

  }

  /**
   * @return The wallet service for the current wallet
   */
  public static Optional<WalletService> getCurrentWalletService() {

    return walletService;

  }

  /**
   * @param walletId The wallet ID for the wallet
   *
   * @return The started wallet service for the given wallet ID
   */
  public static WalletService getOrCreateWalletService(WalletId walletId) {

    log.debug("Get or create wallet service");

    Preconditions.checkNotNull(walletId, "'walletId' must be present");

    // Check if the wallet service has been created for this wallet ID
    File applicationDirectory = InstallationManager.getOrCreateApplicationDataDirectory();

    if (!walletService.isPresent()) {
      walletService = Optional.of(new WalletService(BitcoinNetwork.current().get()));
      walletService.get().initialise(applicationDirectory, walletId);
      walletService.get().start();
    }

    // Return the wallet service
    return walletService.get();

  }

  /**
   * @return The contact service for the current wallet
   */
  public static ContactService getCurrentContactService() {

    log.debug("Get current contact service");

    Optional<WalletSummary> currentWalletSummary = WalletManager.INSTANCE.getCurrentWalletSummary();

    Preconditions.checkState(currentWalletSummary.isPresent(), "'currentWalletSummary' must be present. No wallet is present.");

    WalletId walletId = currentWalletSummary.get().getWalletId();

    return getOrCreateContactService(walletId);
  }


  /**
   * @return The history service for the current wallet
   */
  public static HistoryService getCurrentHistoryService() {

    log.debug("Get current history service");

    Optional<WalletSummary> currentWalletSummary = WalletManager.INSTANCE.getCurrentWalletSummary();

    Preconditions.checkState(currentWalletSummary.isPresent(), "'currentWalletSummary' must be present. No wallet is present.");

    WalletPassword walletPassword = currentWalletSummary.get().getWalletPassword();

    return getOrCreateHistoryService(walletPassword);
  }

  /**
   * @return The history service for a wallet (single soft, multiple hard)
   */
  public static HistoryService getOrCreateHistoryService(WalletPassword walletPassword) {

    log.debug("Get or create history service");

    Preconditions.checkNotNull(walletPassword, "'walletPassword' must be present");

    if (!historyService.isPresent()) {
      historyService = Optional.of(new PersistentHistoryService(walletPassword));
    }

    // Return the existing or new history service
    return historyService.get();

  }

  /**
   * @param walletId The wallet ID for the wallet
   *
   * @return The contact service for a wallet
   */
  public static ContactService getOrCreateContactService(WalletId walletId) {

    log.debug("Get or create contact service");

    Preconditions.checkNotNull(walletId, "'walletId' must be present");

    // Check if the contact service has been created for this wallet ID
    if (!contactService.isPresent()) {
      contactService = Optional.of(new PersistentContactService(walletId));
    }

    // Return the existing or new contact service
    return contactService.get();
  }

  /**
   * <p>Convenience method to log a new history event for the current wallet</p>
   *
   * @param localisedDescription The localised description text
   */
  public static void logHistory(String localisedDescription) {

    // Get the current history service
    HistoryService historyService = CoreServices.getCurrentHistoryService();

    // Create the history entry and persist it
    HistoryEntry historyEntry = historyService.newHistoryEntry(localisedDescription);
    historyService.writeHistory();

    // OK to let everyone else know
    CoreEvents.fireHistoryChangedEvent(historyEntry);

  }

  /**
   * @return A BRIT fee service pointing to the live Matcher machine
   */
  public static FeeService createFeeService() throws CoreException {
    log.debug("Create fee service");

    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    InputStream pgpPublicKeyInputStream = classloader.getResourceAsStream(LIVE_MATCHER_PUBLIC_KEY_FILE);

    try {
      PGPPublicKey matcherPublicKey = PGPUtils.readPublicKey(pgpPublicKeyInputStream);
      URL matcherURL = new URL(LIVE_MATCHER_URL);

      // Return the existing or new fee service
      return new FeeService(matcherPublicKey, matcherURL);
    } catch (Exception e) {
      throw new CoreException(e);
    }
  }
}
