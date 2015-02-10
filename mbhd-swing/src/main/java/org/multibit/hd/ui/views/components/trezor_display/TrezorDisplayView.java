package org.multibit.hd.ui.views.components.trezor_display;

import com.google.common.base.Preconditions;
import net.miginfocom.swing.MigLayout;
import org.multibit.hd.ui.MultiBitUI;
import org.multibit.hd.ui.languages.Languages;
import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.views.components.AbstractComponentView;
import org.multibit.hd.ui.views.components.Labels;
import org.multibit.hd.ui.views.components.Panels;
import org.multibit.hd.ui.views.components.TextBoxes;
import org.multibit.hd.ui.views.themes.Themes;

import javax.swing.*;

/**
 * <p>View to provide the following to UI:</p>
 * <ul>
 * <li>Presentation of a read only Trezor device display</li>
 * <li>Accompanying descriptive operation text</li>
 * </ul>
 *
 * @since 0.0.1
 */
public class TrezorDisplayView extends AbstractComponentView<TrezorDisplayModel> {

  // View components
  private JLabel operationText;
  private JLabel recoveryText;
  private JTextArea deviceDisplayTextArea;
  private JLabel spinner;

  /**
   * @param model The model backing this view
   */
  public TrezorDisplayView(TrezorDisplayModel model) {
    super(model);
  }

  @Override
  public JPanel newComponentPanel() {

    panel = Panels.newPanel(new MigLayout(
      Panels.migXLayout(), // Layout
      "[]", // Columns
      "[]10[]10[]" // Rows
    ));

    // Initialise the components
    operationText = Labels.newCommunicatingWithTrezor();
    recoveryText = Labels.newBlankLabel();
    deviceDisplayTextArea = TextBoxes.newTrezorV1Display(getModel().get().getPanelName());

    // Provide an invisible tar pit spinner
    spinner = Labels.newSpinner(Themes.currentTheme.fadedText(), MultiBitUI.NORMAL_PLUS_ICON_SIZE);
    spinner.setVisible(false);

    // Start the Trezor display as invisible until text is set
    deviceDisplayTextArea.setVisible(false);

    // Add to the panel
    panel.add(operationText, "align center,wrap");
    panel.add(recoveryText, "align center,wrap");
    panel.add(deviceDisplayTextArea, "align center,w 170,wrap");
    panel.add(spinner, "align center,wrap");

    return panel;

  }

  @Override
  public void requestInitialFocus() {
    // Do nothing - components are read only
  }

  @Override
  public void updateModelFromView() {
    // Do nothing - the model is driving the view
  }


  /**
   * <p>Update the operation label with suitable text</p>
   *
   * @param key The message key defining the operation text (e.g. "Communicating with Trezor")
   */
  public void setOperationText(MessageKey key) {

    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "Must be on EDT");

    operationText.setText(Languages.safeText(key));

  }

  /**
   * <p>Update the recovery label with suitable text</p>
   *
   * @param key The message key defining the recovery text (e.g. "Click next to continue" etc)
   */
  public void setRecoveryText(MessageKey key) {

    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "Must be on EDT");

    recoveryText.setText(Languages.safeText(key));

  }

  /**
   * <p>Update the display with suitable text</p>
   *
   * @param key    The message key defining the Trezor text
   * @param values Any supporting values (such as addresses and values)
   */
  public void setDisplayText(MessageKey key, Object... values) {

    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "Must be on EDT.");

    setDisplayVisible(true);
    deviceDisplayTextArea.setText(Languages.safeText(key, values));

  }

  /**
   * <p>Set the visibility of the display text area</p>
   *
   * @param visible True if the display should be visible
   */
  public void setDisplayVisible(boolean visible) {

    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "Must be on EDT");

    deviceDisplayTextArea.setVisible(visible);

  }

  /**
   * <p>Set the visibility of the spinner control</p>
   *
   * @param visible True if the spinner should be visible (such as when a prolonged operation has been invoked)
   */
  public void setSpinnerVisible(boolean visible) {

    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "Must be on EDT");

    spinner.setVisible(visible);

  }

  /**
   * <p>The device has presented incorrect entropy</p>
   */
  public void incorrectEntropy() {

    Preconditions.checkState(SwingUtilities.isEventDispatchThread(), "Must be on EDT");

    setOperationText(MessageKey.TREZOR_FAILURE_OPERATION);

    setDisplayVisible(false);
    setSpinnerVisible(false);

  }
}