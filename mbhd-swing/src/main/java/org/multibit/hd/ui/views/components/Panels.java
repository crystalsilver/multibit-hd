package org.multibit.hd.ui.views.components;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.miginfocom.swing.MigLayout;
import org.multibit.hd.core.dto.CoreMessageKey;
import org.multibit.hd.core.managers.WalletManager;
import org.multibit.hd.ui.MultiBitUI;
import org.multibit.hd.ui.languages.Languages;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.views.components.panels.BackgroundPanel;
import org.multibit.hd.ui.views.components.panels.LightBoxPanel;
import org.multibit.hd.ui.views.components.panels.PanelDecorator;
import org.multibit.hd.ui.views.components.panels.RoundedPanel;
import org.multibit.hd.ui.views.fonts.AwesomeDecorator;
import org.multibit.hd.ui.views.fonts.AwesomeIcon;
import org.multibit.hd.ui.views.themes.Themes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionListener;

/**
 * <p>Factory to provide the following to views:</p>
 * <ul>
 * <li>Creation of panels</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class Panels {

  private static final Logger log = LoggerFactory.getLogger(Panels.class);

  /**
   * A global reference to the application frame
   */
  @SuppressFBWarnings({"MS_CANNOT_BE_FINAL"})
  public static JFrame applicationFrame;

  private static Optional<LightBoxPanel> lightBoxPanel = Optional.absent();

  private static Optional<LightBoxPanel> lightBoxPopoverPanel = Optional.absent();

  /**
   * <p>A default MiG layout constraint with:</p>
   * <ul>
   * <li>Zero insets</li>
   * <li>Fills all available space (X and Y)</li>
   * <li>Handles left-to-right and right-to-left presentation automatically</li>
   * </ul>
   *
   * @return A default MiG layout constraint that fills all X and Y with RTL appended
   */
  public static String migXYLayout() {
    return migLayout("fill,insets 0");
  }

  /**
   * <p>A default MiG layout constraint with:</p>
   * <ul>
   * <li>Zero insets</li>
   * <li>Fills all available space (X only)</li>
   * <li>Handles left-to-right and right-to-left presentation automatically</li>
   * </ul>
   *
   * @return A default MiG layout constraint that fills all X with RTL appended suitable for screens
   */
  public static String migXLayout() {
    return migLayout("fillx,insets 0");
  }

  /**
   * <p>A non-standard MiG layout constraint with:</p>
   * <ul>
   * <li>Optional "fill", "insets", "hidemode" etc</li>
   * <li>Handles left-to-right and right-to-left presentation automatically</li>
   * </ul>
   *
   * @param layout Any of the usual MiG layout constraints except RTL (e.g. "fillx,insets 1 2 3 4")
   *
   * @return The MiG layout constraint with RTL handling appended
   */
  public static String migLayout(String layout) {
    return layout + (Languages.isLeftToRight() ? "" : ",rtl");
  }

  /**
   * <p>A default MiG layout constraint with:</p>
   * <ul>
   * <li>Detail screen insets</li>
   * <li>Fills all available space (X and Y)</li>
   * <li>Handles left-to-right and right-to-left presentation automatically</li>
   * </ul>
   *
   * @return A MiG layout constraint that fills all X and Y with RTL appended suitable for detail views
   */
  public static String migXYDetailLayout() {
    return migLayout("fill,insets 10 5 5 5");
  }

  /**
   * <p>A default MiG layout constraint with:</p>
   * <ul>
   * <li>Popover screen insets</li>
   * <li>Fills all available space (X only)</li>
   * <li>Handles left-to-right and right-to-left presentation automatically</li>
   * </ul>
   *
   * @return A MiG layout constraint that fills all X with RTL appended suitable for popovers
   */
  public static String migXPopoverLayout() {
    return migLayout("fill,insets 10 10 10 10");
  }

  /**
   * @return A simple theme-aware panel with a single cell MigLayout that fills all X and Y
   */
  public static JPanel newPanel() {

    return Panels.newPanel(
      new MigLayout(
        migXYLayout(), // Layout
        "[]", // Columns
        "[]" // Rows
      ));

  }

  /**
   * @param layout The layout manager for the panel (typically MigLayout)
   *
   * @return A simple theme-aware detail panel with the given layout
   */
  public static JPanel newPanel(LayoutManager2 layout) {

    JPanel panel = new JPanel(layout);

    // Theme
    panel.setBackground(Themes.currentTheme.detailPanelBackground());

    // Force transparency
    panel.setOpaque(false);

    // Ensure LTR and RTL is detected by the layout
    panel.applyComponentOrientation(Languages.currentComponentOrientation());

    return panel;

  }

  /**
   * @return A simple panel with rounded corners and a single column MigLayout
   */
  public static JPanel newRoundedPanel() {

    return newRoundedPanel(
      new MigLayout(
        Panels.migXLayout(),
        "[]", // Columns
        "[]" // Rows
      ));

  }

  /**
   * @param layout The MiGLayout to use
   *
   * @return A simple panel with rounded corners
   */
  public static JPanel newRoundedPanel(LayoutManager2 layout) {

    JPanel panel = new RoundedPanel(layout);

    // Theme
    panel.setBackground(Themes.currentTheme.detailPanelBackground());
    panel.setForeground(Themes.currentTheme.fadedText());

    return panel;

  }

  /**
   * @param icon The Awesome icon to use as the basis of the image for consistent LaF
   *
   * @return A theme-aware panel with rounded corners and a single cell MigLayout
   */
  public static BackgroundPanel newDetailBackgroundPanel(AwesomeIcon icon) {

    // Create an image from the AwesomeIcon
    Image image = ImageDecorator.toImageIcon(
      AwesomeDecorator.createIcon(
        icon,
        Themes.currentTheme.fadedText(),
        MultiBitUI.HUGE_ICON_SIZE
      )).getImage();

    BackgroundPanel panel = new BackgroundPanel(image, BackgroundPanel.ACTUAL);

    panel.setLayout(
      new MigLayout(
        Panels.migXLayout(),
        "[]", // Columns
        "[]" // Rows
      ));
    panel.setAlpha(MultiBitUI.DETAIL_PANEL_BACKGROUND_ALPHA);
    panel.setPaint(Themes.currentTheme.detailPanelBackground());
    panel.setBackground(Themes.currentTheme.detailPanelBackground());

    return panel;

  }

  /**
   * <p>Test if a light box is showing</p>
   *
   * @return True if the light box panel is visible
   */
  public synchronized static boolean isLightBoxShowing() {

    return lightBoxPanel.isPresent();

  }

  /**
   * <p>Show a light box</p>
   *
   * @param panel The panel to act as the focus of the light box
   */
  public synchronized static void showLightBox(JPanel panel) {

    log.debug("Show light box");

    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "LightBox requires the EDT");

    // Do not override this to replace the existing light box
    // The problem is that the new light box is tripping up due to a race condition in the code
    // which needs to be dealt with rather than masked behind deferred clean up
    Preconditions.checkState(!lightBoxPanel.isPresent(), "Light box should never be called twice ");

    // Prevent focus
    allowFocus(Panels.applicationFrame, false);

    // Add the light box panel
    lightBoxPanel = Optional.of(new LightBoxPanel(panel, JLayeredPane.MODAL_LAYER));

  }

  /**
   * <p>Hides the currently showing light box panel (and any popover)</p>
   */
  public synchronized static void hideLightBoxIfPresent() {

    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "LightBoxPopover requires the EDT");

    log.debug("Hide light box (if present)");

    hideLightBoxPopoverIfPresent();

    if (lightBoxPanel.isPresent()) {
      lightBoxPanel.get().close();
    }

    lightBoxPanel = Optional.absent();

    // Finally allow focus
    allowFocus(Panels.applicationFrame, true);

  }

  /**
   * <p>Test if a light box popover is showing</p>
   *
   * @return True if the popover panel is visible
   */
  public synchronized static boolean isLightBoxPopoverShowing() {

    return lightBoxPopoverPanel.isPresent();

  }

  /**
   * <p>Show a light box pop over</p>
   *
   * @param panel The panel to act as the focus of the popover
   */
  public synchronized static void showLightBoxPopover(JPanel panel) {

    log.debug("Show light box popover");

    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "LightBoxPopover requires the EDT");
    Preconditions.checkState(lightBoxPanel.isPresent(), "LightBoxPopover should not be called unless a light box is showing");
    Preconditions.checkState(!lightBoxPopoverPanel.isPresent(), "LightBoxPopover should never be called twice");

    lightBoxPopoverPanel = Optional.of(new LightBoxPanel(panel, JLayeredPane.DRAG_LAYER));

  }

  /**
   * <p>Hides the currently showing light box popover panel</p>
   */
  public synchronized static void hideLightBoxPopoverIfPresent() {

    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "LightBoxPopover requires the EDT");

    log.debug("Hide light box popover (if present)");

    if (lightBoxPopoverPanel.isPresent()) {
      // A popover is not part of a handover so gets closed completely
      lightBoxPopoverPanel.get().close();
    }

    lightBoxPopoverPanel = Optional.absent();

  }

  /**
   * <p>An "exit selector" panel confirms an exit or switch operation</p>
   *
   * @param listener      The action listener
   * @param exitCommand   The exit command name
   * @param switchCommand The switch command name
   *
   * @return A new "exit selector" panel
   */
  public static JPanel newExitSelector(
    ActionListener listener,
    String exitCommand,
    String switchCommand
  ) {

    JPanel panel = Panels.newPanel();

    JRadioButton radio1 = RadioButtons.newRadioButton(listener, MessageKey.EXIT_WALLET);
    radio1.setSelected(true);
    radio1.setActionCommand(exitCommand);

    JRadioButton radio2 = RadioButtons.newRadioButton(listener, MessageKey.SWITCH_WALLET);
    radio2.setActionCommand(switchCommand);

    // Wallet selection is mutually exclusive
    ButtonGroup group = new ButtonGroup();
    group.add(radio1);
    group.add(radio2);

    // Add to the panel
    panel.add(radio1, "wrap");
    panel.add(radio2, "wrap");

    return panel;
  }

  /**
   * <p>A "licence selector" panel provides a means of ensuring the user agrees with the licence</p>
   *
   * @param listener        The action listener
   * @param agreeCommand    The agree command name
   * @param disagreeCommand The disagree command name
   *
   * @return A new "licence selector" panel
   */
  public static JPanel newLicenceSelector(
    ActionListener listener,
    String agreeCommand,
    String disagreeCommand
  ) {

    JPanel panel = Panels.newPanel();

    JRadioButton radio1 = RadioButtons.newRadioButton(listener, MessageKey.ACCEPT_LICENCE);
    radio1.setActionCommand(agreeCommand);

    JRadioButton radio2 = RadioButtons.newRadioButton(listener, MessageKey.REJECT_LICENCE);
    radio2.setSelected(true);
    radio2.setActionCommand(disagreeCommand);

    // Wallet selection is mutually exclusive
    ButtonGroup group = new ButtonGroup();
    group.add(radio1);
    group.add(radio2);

    // Add to the panel
    panel.add(radio1, "wrap");
    panel.add(radio2, "wrap");

    return panel;
  }

  /**
   * <p>A standard "wallet selector" panel provides a means of choosing how a wallet is to be created/accessed</p>
   *
   * @param listener               The action listener
   * @param createCommand          The create command name
   * @param existingWalletCommand  The existing wallet command name
   * @param restorePasswordCommand The restore credentials command name
   * @param restoreWalletCommand   The restore wallet command name
   *
   * @return A new "wallet selector" panel
   */
  public static JPanel newWalletSelector(
    ActionListener listener,
    String createCommand,
    String existingWalletCommand,
    String restorePasswordCommand,
    String restoreWalletCommand
  ) {

    JPanel panel = Panels.newPanel();

    JRadioButton radio1 = RadioButtons.newRadioButton(listener, MessageKey.CREATE_WALLET);
    radio1.setSelected(true);
    radio1.setActionCommand(createCommand);

    JRadioButton radio2 = RadioButtons.newRadioButton(listener, MessageKey.USE_EXISTING_WALLET);
    radio2.setActionCommand(existingWalletCommand);

    JRadioButton radio3 = RadioButtons.newRadioButton(listener, MessageKey.RESTORE_PASSWORD);
    radio3.setActionCommand(restorePasswordCommand);

    JRadioButton radio4 = RadioButtons.newRadioButton(listener, MessageKey.RESTORE_WALLET);
    radio4.setActionCommand(restoreWalletCommand);

    // Check for existing wallets
    if (WalletManager.getSoftWalletSummaries().isEmpty()) {
      radio2.setEnabled(false);
      radio2.setForeground(UIManager.getColor("RadioButton.disabledText"));
    }

    // Wallet selection is mutually exclusive
    ButtonGroup group = new ButtonGroup();
    group.add(radio1);
    group.add(radio2);
    group.add(radio3);
    group.add(radio4);

    // Add to the panel
    panel.add(radio1, "wrap");
    panel.add(radio2, "wrap");
    panel.add(radio3, "wrap");
    panel.add(radio4, "wrap");

    return panel;
  }

  /**
   * <p>A "hardware wallet selector" panel provides a means of choosing how a hardware wallet is to be created/accessed</p>
   *
   * @param listener              The action listener
   * @param createCommand         The create command name
   * @param existingWalletCommand The existing wallet command name
   * @param restoreWalletCommand  The restore wallet command name
   *
   * @return A new "wallet selector" panel
   */
  public static JPanel newHardwareWalletSelector(
    ActionListener listener,
    String createCommand,
    String existingWalletCommand,
    String restoreWalletCommand
  ) {

    JPanel panel = Panels.newPanel();

    JRadioButton radio1 = RadioButtons.newRadioButton(listener, MessageKey.TREZOR_CREATE_WALLET);
    radio1.setSelected(true);
    radio1.setActionCommand(createCommand);

    JRadioButton radio2 = RadioButtons.newRadioButton(listener, MessageKey.USE_EXISTING_WALLET);
    radio2.setActionCommand(existingWalletCommand);

    JRadioButton radio3 = RadioButtons.newRadioButton(listener, MessageKey.RESTORE_WALLET);
    radio3.setActionCommand(restoreWalletCommand);

    // Check for existing wallets
    if (WalletManager.getWalletSummaries().isEmpty()) {
      radio2.setEnabled(false);
      radio2.setForeground(UIManager.getColor("RadioButton.disabledText"));
    }

    // Wallet selection is mutually exclusive
    ButtonGroup group = new ButtonGroup();
    group.add(radio1);
    group.add(radio2);
    group.add(radio3);

    // Add to the panel
    panel.add(radio1, "wrap");
    panel.add(radio2, "wrap");
    panel.add(radio3, "wrap");

    return panel;
  }

  /**
   * <p>A "Trezor select PIN" panel provides a means of choosing how a device PIN is to be changed/removed</p>
   *
   * @param listener      The action listener
   * @param changeCommand The change PIN command name
   * @param removeCommand The remove PIN command name
   *
   * @return A new "wallet selector" panel
   */
  public static JPanel newChangePinSelector(
    ActionListener listener,
    String changeCommand,
    String removeCommand
  ) {

    JPanel panel = Panels.newPanel();

    JRadioButton radio1 = RadioButtons.newRadioButton(listener, MessageKey.CHANGE_PIN_OPTION);
    radio1.setSelected(true);
    radio1.setActionCommand(changeCommand);

    JRadioButton radio2 = RadioButtons.newRadioButton(listener, MessageKey.REMOVE_PIN_OPTION);
    radio2.setActionCommand(removeCommand);

    // Wallet selection is mutually exclusive
    ButtonGroup group = new ButtonGroup();
    group.add(radio1);
    group.add(radio2);

    // Add to the panel
    panel.add(radio1, "wrap");
    panel.add(radio2, "wrap");

    return panel;
  }

  /**
   * <p>A "Trezor tool selector" panel provides a means of choosing which Trezor tool to run</p>
   *
   * @param listener            The action listener
   * @param buyTrezorCommand    The buy trezor command
   * @param verifyDeviceCommand The verify device command name
   * @param wipeDeviceCommand   The wipe device command name
   *
   * @return A new "use Trezor selector" panel
   */
  public static JPanel newUseTrezorSelector(
    ActionListener listener,
    String buyTrezorCommand,
    String verifyDeviceCommand,
    String wipeDeviceCommand
  ) {

    JPanel panel = Panels.newPanel();

    JRadioButton radio1 = RadioButtons.newRadioButton(listener, MessageKey.BUY_TREZOR);
    radio1.setActionCommand(buyTrezorCommand);
    radio1.setSelected(true);

    JRadioButton radio2 = RadioButtons.newRadioButton(listener, MessageKey.VERIFY_DEVICE);
    radio2.setActionCommand(verifyDeviceCommand);

    JRadioButton radio3 = RadioButtons.newRadioButton(listener, MessageKey.WIPE_DEVICE);
    radio3.setActionCommand(wipeDeviceCommand);

    // Action selection is mutually exclusive
    ButtonGroup group = new ButtonGroup();
    group.add(radio1);
    group.add(radio2);
    group.add(radio3);

    // Add to the panel
    panel.add(radio1, "wrap");
    panel.add(radio2, "wrap");
    panel.add(radio3, "wrap");

    return panel;
  }

  /**
   * <p>A "confirm seed phrase" panel displays the instructions to enter the seed phrase from a piece of paper</p>
   *
   * @return A new "confirm seed phrase" panel
   */
  public static JPanel newConfirmSeedPhrase() {

    JPanel panel = Panels.newPanel(
      new MigLayout(
        Panels.migXYLayout(),
        "[]", // Columns
        "[]" // Rows
      ));

    // Add to the panel
    panel.add(Labels.newConfirmSeedPhraseNote(), "grow,push");

    return panel;
  }

  /**
   * <p>A "debugger warning" panel displays instructions to the user about a debugger being attached</p>
   *
   * @return A new "debugger warning" panel
   */
  public static JPanel newDebuggerWarning() {

    JPanel panel = Panels.newPanel(
      new MigLayout(
        Panels.migXLayout(),
        "[]", // Columns
        "[]" // Rows
      ));

    // Ensure it is accessible
    AccessibilityDecorator.apply(panel, CoreMessageKey.DEBUGGER_ATTACHED);

    PanelDecorator.applyDangerFadedTheme(panel);

    // Add to the panel
    panel.add(Labels.newDebuggerWarningNote(), "w 350");

    return panel;
  }

  /**
   * <p>An "unsupported firmware" panel displays instructions to the user about a hardware wallet with unsupported firmware (security risk) being attached</p>
   *
   * @return A new "unsupported firmware" panel
   */
  public static JPanel newUnsupportedFirmware() {

    JPanel panel = Panels.newPanel(
      new MigLayout(
        Panels.migXLayout(),
        "[]", // Columns
        "[]" // Rows
      ));

    // Ensure it is accessible
    AccessibilityDecorator.apply(panel, CoreMessageKey.UNSUPPORTED_FIRMWARE_ATTACHED);

    PanelDecorator.applyWarningTheme(panel);

    // Add to the panel
    panel.add(Labels.newUnsupportedFirmwareNote(), "w 350");

    return panel;
  }

  /**
   * <p>A "language change" panel displays instructions to the user about a language change</p>
   *
   * @return A new "language change" panel
   */
  public static JPanel newLanguageChange() {

    JPanel panel = Panels.newPanel(
      new MigLayout(
        Panels.migXLayout(),
        "[]", // Columns
        "[]" // Rows
      ));

    PanelDecorator.applySuccessFadedTheme(panel);

    // Add to the panel
    panel.add(Labels.newLanguageChangeNote(), "grow,push");

    return panel;
  }

  /**
   * <p>A "restore from backup" panel displays the instructions to restore from a backup folder</p>
   *
   * @return A new "restore from backup" panel
   */
  public static JPanel newRestoreFromBackup() {

    JPanel panel = Panels.newPanel(
      new MigLayout(
        Panels.migXLayout(),
        "[]", // Columns
        "[]" // Rows
      ));

    // Add to the panel
    panel.add(Labels.newRestoreFromBackupNote(), "grow,push");

    return panel;
  }

  /**
   * <p>A "restore from seed phrase" panel displays the instructions to restore from a seed phrase</p>
   *
   * @return A new "restore from seed phrase" panel
   */
  public static JPanel newRestoreFromSeedPhrase() {

    JPanel panel = Panels.newPanel(
      new MigLayout(
        Panels.migXLayout(),
        "[]", // Columns
        "[]" // Rows
      ));

    // Add to the panel
    panel.add(Labels.newRestoreFromSeedPhraseNote(), "grow,push");

    return panel;
  }

  /**
   * <p>A "select backup directory" panel displays the instructions to choose an appropriate backup directory</p>
   *
   * @return A new "select backup directory" panel
   */
  public static JPanel newSelectBackupDirectory() {

    JPanel panel = Panels.newPanel(
      new MigLayout(
        Panels.migXLayout(),
        "[]", // Columns
        "[]" // Rows
      ));

    // Add to the panel
    panel.add(Labels.newSelectBackupLocationNote(), "grow,push");

    return panel;
  }

  /**
   * <p>A "select export payments directory" panel displays the instructions to choose an appropriate export payments directory</p>
   *
   * @return A new "select export payments directory" panel
   */
  public static JPanel newSelectExportPaymentsDirectory() {

    JPanel panel = Panels.newPanel(
      new MigLayout(
        Panels.migXLayout(),
        "[]", // Columns
        "[]" // Rows
      ));

    // Add to the panel
    panel.add(Labels.newSelectExportPaymentsLocationNote(), "grow,push");

    return panel;
  }

  /**
   * New vertical dashed separator
   */
  public static JPanel newVerticalDashedSeparator() {

    JPanel panel = new JPanel();
    panel.setMaximumSize(new Dimension(1, 10000));
    panel.setBorder(BorderFactory.createDashedBorder(Themes.currentTheme.headerPanelBackground(), 5, 5));

    return panel;
  }

  /**
   * <p>Invalidate a panel so that Swing will later redraw it properly with layout changes (normally as a result of a locale change)</p>
   *
   * @param panel The panel to invalidate
   */
  public static void invalidate(JPanel panel) {

    // Added new content so validate/repaint
    panel.validate();
    panel.repaint();

  }

  /**
   * <p>Recursive method to enable or disable the focus on all components in the given container</p>
   * <p>Filters components that cannot have focus by design (e.g. JLabel)</p>
   *
   * @param component  The component
   * @param allowFocus True if the components should be able to gain focus
   */
  private static void allowFocus(final Component component, final boolean allowFocus) {

    // Limit the focus change to those components that could grab it
    if (component instanceof AbstractButton) {
      component.setFocusable(allowFocus);
    }
    if (component instanceof JComboBox) {
      component.setFocusable(allowFocus);
    }
    if (component instanceof JTree) {
      component.setFocusable(allowFocus);
    }
    if (component instanceof JTextComponent) {
      component.setFocusable(allowFocus);
    }
    if (component instanceof JTable) {
      component.setFocusable(allowFocus);
    }

    // Recursive search
    if (component instanceof Container) {
      for (Component child : ((Container) component).getComponents()) {
        allowFocus(child, allowFocus);
      }

    }

  }

}
