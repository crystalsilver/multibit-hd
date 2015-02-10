package org.multibit.hd.core.dto;

import com.google.common.base.Optional;

/**
 * <p>Value object to provide the following to Core API:</p>
 * <ul>
 * <li>Information about the Bitcoin network status</li>
 * </ul>
 *
 * @since 0.0.1
 *
 */
public class SecuritySummary {

  public enum AlertType {

    DEBUGGER_ATTACHED,
    SYSTEM_TIME_DRIFT,
    BACKUP_FAILED,
    CERTIFICATE_FAILED,
    UNSUPPORTED_FIRMWARE_ATTACHED,

    // End of enum
    ;

  }

  private final AlertType alertType;
  private final RAGStatus severity;

  private final Optional<CoreMessageKey> messageKey;
  private final Optional<Object[]> messageData;


  /**
   * <p>A debugger is attached - could be an attack in progress</p>
   *
   * @return A new "debugger attached" summary
   */
  public static SecuritySummary newDebuggerAttached() {
    return new SecuritySummary(
      RAGStatus.RED,
      Optional.of(CoreMessageKey.DEBUGGER_ATTACHED),
      Optional.<Object[]>absent(),
      AlertType.DEBUGGER_ATTACHED
    );
  }

  /**
   * <p>The system time has drifted - could cause transaction problems</p>
   *
   * @return A new "system time drift" summary
   */
  public static SecuritySummary newSystemTimeDrift() {
    return new SecuritySummary(
      RAGStatus.AMBER,
      Optional.of(CoreMessageKey.SYSTEM_TIME_DRIFT),
      Optional.<Object[]>absent(),
      AlertType.SYSTEM_TIME_DRIFT
    );
  }

  /**
   * <p>A ZIP backup has failed to write correctly</p>
   *
   * @return A new "backup failed" summary
   */
  public static SecuritySummary newBackupFailed() {
    return new SecuritySummary(
      RAGStatus.AMBER,
      Optional.of(CoreMessageKey.BACKUP_FAILED),
      Optional.<Object[]>absent(),
      AlertType.BACKUP_FAILED
    );
  }

  /**
   * <p>A HTTPS connection has failed due to incorrect certificate</p>
   *
   * @return A new "certificate failure" summary
   */
  public static SecuritySummary newCertificateFailed() {
    return new SecuritySummary(
      RAGStatus.RED,
      Optional.of(CoreMessageKey.CERTIFICATE_FAILED),
      Optional.<Object[]>absent(),
      AlertType.CERTIFICATE_FAILED
    );
  }

  /**
   * <p>A hardware wallet with unsupported firmware is attached - could expose user to risk</p>
   *
   * @return A new "unsupported firmware" summary
   */
  public static SecuritySummary newUnsupportedFirmware() {
    return new SecuritySummary(
      RAGStatus.AMBER,
      Optional.of(CoreMessageKey.UNSUPPORTED_FIRMWARE_ATTACHED),
      Optional.<Object[]>absent(),
      AlertType.UNSUPPORTED_FIRMWARE_ATTACHED
    );
  }

  /**
   * @param severity    The severity (Red, Amber, Green)
   * @param messageKey  The error key to allow localisation
   * @param messageData The error data for insertion into the error message
   */
  public SecuritySummary(
    RAGStatus severity,
    Optional<CoreMessageKey> messageKey,
    Optional<Object[]> messageData,
    AlertType alertType
  ) {

    this.alertType = alertType;
    this.severity = severity;

    this.messageKey = messageKey;
    this.messageData = messageData;
  }

  /**
   * @return The alert type to assist creation of alert models
   */
  public AlertType getAlertType() {
    return alertType;
  }

  /**
   * @return The severity (e.g. AMBER)
   */
  public RAGStatus getSeverity() {
    return severity;
  }

  /**
   * @return The message key
   */
  public Optional<CoreMessageKey> getMessageKey() {
    return messageKey;
  }

  /**
   * @return An optional array of arbitrary objects, often for insertion into a resource bundle string
   */
  public Optional<Object[]> getMessageData() {
    return messageData;
  }

  @Override
  public String toString() {
    return "SecuritySummary{" +
      "alertType=" + alertType +
      ", severity=" + severity +
      ", messageKey=" + messageKey +
      ", messageData=" + messageData +
      '}';
  }
}
