package org.multibit.hd.ui;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.Uninterruptibles;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.multibit.hd.core.concurrent.SafeExecutors;
import org.multibit.hd.core.config.Configurations;
import org.multibit.hd.core.events.CoreEvents;
import org.multibit.hd.core.events.ShutdownEvent;
import org.multibit.hd.core.managers.InstallationManager;
import org.multibit.hd.core.managers.SSLManager;
import org.multibit.hd.core.managers.WalletManager;
import org.multibit.hd.core.services.CoreServices;
import org.multibit.hd.core.utils.OSUtils;
import org.multibit.hd.hardware.core.HardwareWalletService;
import org.multibit.hd.ui.audio.Sounds;
import org.multibit.hd.ui.controllers.HeaderController;
import org.multibit.hd.ui.controllers.MainController;
import org.multibit.hd.ui.events.controller.ControllerEvents;
import org.multibit.hd.ui.events.view.ViewEvents;
import org.multibit.hd.ui.platform.GenericApplicationFactory;
import org.multibit.hd.ui.platform.GenericApplicationSpecification;
import org.multibit.hd.ui.services.BitcoinURIListeningService;
import org.multibit.hd.ui.views.MainView;
import org.multibit.hd.ui.views.themes.ThemeKey;
import org.multibit.hd.ui.views.themes.Themes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javax.swing.text.DefaultEditorKit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>Main entry point to the application</p>
 */
public class MultiBitHD {

  private static final Logger log = LoggerFactory.getLogger(MultiBitHD.class);

  private MainController mainController;

  private final ListeningExecutorService cacertsExecutorService = SafeExecutors.newSingleThreadExecutor("install-cacerts");

  /**
   * <p>Main entry point to the application</p>
   *
   * @param args None specified
   */
  public static void main(String[] args) throws Exception {

    if (args != null) {
      // Show the command line arguments
      for (int i = 0; i < args.length; i++) {
        log.debug("MultiBit launched with args[{}]: '{}'", i, args[i]);
      }
    } else {
      // Provide empty arguments to avoid potential NPEs
      args = new String[]{};
    }

    // Hand over to an instance to simplify FEST tests
    final MultiBitHD multiBitHD = new MultiBitHD();
    if (!multiBitHD.start(args)) {

      // Failed to start so issue a hard shutdown
      CoreServices.shutdownNow(ShutdownEvent.ShutdownType.HARD);

    } else {

      // Initialise the UI views in the EDT
      SwingUtilities.invokeLater(
        new Runnable() {
          @Override
          public void run() {

            multiBitHD.initialiseUIViews();

          }
        });

    }

    log.debug("Bootstrap complete.");

  }

  public void stop() {

    log.debug("Stopping DigiBit HD");

    mainController = null;

    // final purge in case anything gets missed
    ViewEvents.unsubscribeAll();
    ControllerEvents.unsubscribeAll();
    CoreEvents.unsubscribeAll();

  }

  /**
   * <p>Start this instance of MultiBit HD</p>
   *
   * @param args The command line arguments
   *
   * @return True if the application started successfully, false if a shutdown is required
   *
   * @throws Exception If something goes wrong
   */
  public boolean start(String[] args) throws Exception {

    // Prepare the JVM (Nimbus, system properties etc)
    initialiseJVM();

    // Start core services (logging, security alerts, configuration, Bitcoin URI handling etc)
    initialiseCore(args);

    // Create controllers so that the generic app can access listeners
    if (!initialiseUIControllers(args)) {

      // Required to shut down
      return false;

    }

    // Prepare platform-specific integration (protocol handlers, quit events etc)
    initialiseGenericApp();

    // Must be OK to be here
    return true;
  }

  /**
   * <p>Initialise the JVM. This occurs before anything else is called.</p>
   */
  // Calling exit(-1) is required
  @SuppressFBWarnings({"DM_EXIT"})
  private void initialiseJVM() throws Exception {

    log.debug("Initialising JVM...");

    // Although we guarantee the JVM through the packager it is possible that
    // a power user will use their own
    try {
      // We guarantee the JVM through the packager so we should try it first
      UIManager.setLookAndFeel(new NimbusLookAndFeel());
    } catch (UnsupportedLookAndFeelException e) {
      try {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e1) {
        log.error("No look and feel available. DigiBit HD requires Java 7 or higher.", e1);
        System.exit(-1);
      }
    }

    // Set any bespoke system properties
    try {
      // Fix for Windows / Java 7 / VPN bug
      System.setProperty("java.net.preferIPv4Stack", "true");

      // Fix for version.txt not visible for Java 7
      System.setProperty("jsse.enableSNIExtension", "false");

      if (OSUtils.isMac()) {

        // Ensure the correct name is displayed in the application menu
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", "DigiBit HD");

        // Ensure OSX key bindings are used for copy, paste etc
        // Use the Nimbus keys and ensure this occurs before any component creation
        addOSXKeyStrokes((InputMap) UIManager.get("TextField.focusInputMap"));
        addOSXKeyStrokes((InputMap) UIManager.get("FormattedTextField.focusInputMap"));
        addOSXKeyStrokes((InputMap) UIManager.get("TextArea.focusInputMap"));
        addOSXKeyStrokes((InputMap) UIManager.get("PasswordField.focusInputMap"));
        addOSXKeyStrokes((InputMap) UIManager.get("EditorPane.focusInputMap"));

      }

      // Execute the CA certificates download on a separate thread to avoid slowing
      // the startup time
      cacertsExecutorService.submit(
        new Runnable() {
          @Override
          public void run() {
            SSLManager.INSTANCE.installCACertificates(
              InstallationManager.getOrCreateApplicationDataDirectory(),
              InstallationManager.CA_CERTS_NAME,
              false // Do not force loading if they are already present
            );

          }
        });


    } catch (SecurityException se) {
      log.error(se.getClass().getName() + " " + se.getMessage());
    }

  }

  /**
   * <p>Initialise the UI controllers once all the core services are in place</p>
   * <p>This creates the singleton controllers that respond to generic events</p>
   * <p>At this stage none of the following will be running:</p>
   * <ul>
   * <li>Themes or views</li>
   * <li>Wallet service</li>
   * <li>Backup service</li>
   * <li>Bitcoin network service</li>
   * </ul>
   */
  public boolean initialiseUIControllers(String[] args) {

    // Determine if another instance is running and shutdown if this is the case
    BitcoinURIListeningService bitcoinURIListeningService = new BitcoinURIListeningService(args);
    if (!bitcoinURIListeningService.start()) {
      return false;
    }

    if (OSUtils.isWindowsXPOrEarlier()) {
      log.error("Windows XP or earlier detected. Forcing shutdown.");
      JOptionPane.showMessageDialog(
        null, "This version of Windows is not supported for security reasons.\nPlease upgrade.", "Error",
        JOptionPane.ERROR_MESSAGE);
      return false;
    }

    // Including the other controllers avoids dangling references during a soft shutdown
    mainController = new MainController(
      bitcoinURIListeningService,
      new HeaderController()
    );

    // Start the hardware wallet support to allow credentials screen to be selected
    mainController.handleHardwareWallets();

    // Set the tooltip delay to be slightly longer
    ToolTipManager.sharedInstance().setInitialDelay(1000);

    // Must be OK to be here
    return true;

  }

  /**
   * <p>Apply OSX key strokes to input map for consistent UX</p>
   *
   * @param inputMap The input map
   */
  private void addOSXKeyStrokes(InputMap inputMap) {

    // Undo and redo require more complex handling
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), DefaultEditorKit.selectAllAction);

  }

  /**
   * <p>Initialise the platform-specific services</p>
   */
  private void initialiseGenericApp() {

    GenericApplicationSpecification specification = new GenericApplicationSpecification();
    specification.getOpenURIEventListeners().add(mainController);
    specification.getPreferencesEventListeners().add(mainController);
    specification.getAboutEventListeners().add(mainController);
    specification.getQuitEventListeners().add(mainController);

    GenericApplicationFactory.INSTANCE.buildGenericApplication(specification);

  }

  /**
   * <p>Initialise the core services</p>
   *
   * @param args The command line arguments
   */
  private void initialiseCore(String[] args) {

    log.debug("Initialising Core...");

    // Start the core services
    CoreServices.main(args);

    // Pre-loadContacts sound library
    Sounds.initialise();

  }

  /**
   * <p>Initialise the UI once all the core services are in place</p>
   * <p>This creates the singleton views and controllers that respond to configuration
   * and theme changes</p>
   * <p>At this stage none of the following will be running:</p>
   * <ul>
   * <li>Wallet service</li>
   * <li>Backup service</li>
   * <li>Bitcoin network service</li>
   * </ul>
   * <p>Once the UI renders, control passes to the <code>MainController</code> to
   * respond to the wizard close event which will trigger ongoing initialisation.</p>
   */
  public MainView initialiseUIViews() {

    log.debug("Initialising UI...");

    Preconditions.checkNotNull(mainController, "'mainController' must be present. FEST will cause this if another instance is running.");

    log.debug("Switching theme...");

    // Ensure that we are using the configured theme
    ThemeKey themeKey = ThemeKey.valueOf(Configurations.currentConfiguration.getAppearance().getCurrentTheme());
    Themes.switchTheme(themeKey.theme());

    final Optional<HardwareWalletService> hardwareWalletService = CoreServices.getOrCreateHardwareWalletService();
    if (hardwareWalletService.isPresent()) {
      // Give MultiBit Hardware a chance to process any attached hardware wallet
      // and for MainController to subsequently process the events
      // The delay observed in reality and FEST tests ranges from 1400-2200ms and if
      // not included results in wiped hardware wallets being missed on startup
      log.debug("Allowing time for hardware wallet state transition");
      Uninterruptibles.sleepUninterruptibly(2, TimeUnit.SECONDS);
    }

    log.debug("Building MainView...");

    // Build a new MainView
    final MainView mainView = new MainView();
    mainController.setMainView(mainView);

    log.debug("Checking for pre-existing wallets...");

    // Check for any pre-existing wallets in the application directory
    File applicationDataDirectory = InstallationManager.getOrCreateApplicationDataDirectory();
    List<File> walletDirectories = WalletManager.findWalletDirectories(applicationDataDirectory);

    // Check for fresh install
    boolean showWelcomeWizard = walletDirectories.isEmpty() || !Configurations.currentConfiguration.isLicenceAccepted();

    if (showWelcomeWizard) {
      log.debug("Wallet directory is empty or no licence accepted");
    }

    // Check for fresh hardware wallet
    if (hardwareWalletService.isPresent()) {

      if (hardwareWalletService.get().isDeviceReady() && !hardwareWalletService.get().isWalletPresent()) {

        log.debug("Wiped hardware wallet detected");

        // Must show the welcome wizard in hardware wallet mode
        // regardless of wallet or licence situation
        // MainController should have handled the events
        showWelcomeWizard = true;

      }

    }

    if (showWelcomeWizard) {

      log.debug("Showing the welcome wizard");
      mainView.setShowExitingWelcomeWizard(true);
      mainView.setShowExitingCredentialsWizard(false);

    } else {

      log.debug("Showing the credentials wizard");
      mainView.setShowExitingCredentialsWizard(true);
      mainView.setShowExitingWelcomeWizard(false);

    }

    // Provide a backdrop to the user and trigger the showing of the wizard
    mainView.refresh();

    log.debug("Initialising UI: Refresh complete");

    // See the MainController wizard hide event for the next stage

    return mainView;

  }
}
