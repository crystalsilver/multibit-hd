package org.multibit.hd.ui.services;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import org.bitcoinj.core.Address;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multibit.hd.core.events.CoreEvents;
import org.multibit.hd.core.events.ShutdownEvent;
import org.multibit.hd.core.managers.InstallationManager;

import java.io.InputStreamReader;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import static junit.framework.TestCase.fail;
import static org.fest.assertions.Assertions.assertThat;

public class BitcoinURIListeningServiceTest {

  private static final String RAW_URI_FULL = "bitcoin:1AhN6rPdrMuKBGFDKR1k9A8SCLYaNgXhty?amount=0.01&label=Please%20donate%20to%20multibit.org";
  private static final String RAW_URI_ADDRESS = "bitcoin:1AhN6rPdrMuKBGFDKR1k9A8SCLYaNgXhty";

  private ServerSocket serverSocket = null;

  private BitcoinURIListeningService testObject;

  @Before
  public void setUp() throws Exception {

    // Ensure the shutdown event doesn't overwrite existing configuration
    InstallationManager.unrestricted = true;

  }

  @After
  public void tearDown() throws Exception {

    if (serverSocket != null) {

      serverSocket.close();

    }

  }

  @Test
  public void testParseRawURI_URLEncoded() throws Exception {

    String[] args = new String[]{
      RAW_URI_FULL
    };

    testObject = new BitcoinURIListeningService(args);

    Address address = testObject.getBitcoinURI().get().getAddress();
    if (address == null) {
      fail();
    }
    assertThat(address.toString()).isEqualTo("1AhN6rPdrMuKBGFDKR1k9A8SCLYaNgXhty");

    // Don't crash the JVM
    CoreEvents.fireShutdownEvent(ShutdownEvent.ShutdownType.SOFT);

    assertThat(testObject.getServerSocket().isPresent()).isFalse();

  }

  @Test
  public void testParseRawURI_AddressOnly() throws Exception {

    String[] args = new String[]{
      RAW_URI_ADDRESS
    };

    testObject = new BitcoinURIListeningService(args);

    Address address = testObject.getBitcoinURI().get().getAddress();
    if (address == null) {
      fail();
    }
    assertThat(address.toString()).isEqualTo("1AhN6rPdrMuKBGFDKR1k9A8SCLYaNgXhty");

    // Don't crash the JVM
    CoreEvents.fireShutdownEvent(ShutdownEvent.ShutdownType.SOFT);

    assertThat(testObject.getServerSocket().isPresent()).isFalse();

  }

  @Test
  public void testNotify_Full() throws Exception {

    serverSocket = new ServerSocket(
      BitcoinURIListeningService.MULTIBIT_HD_NETWORK_SOCKET,
      10,
      InetAddress.getLoopbackAddress()
    );

    String[] args = new String[]{
      RAW_URI_FULL
    };

    testObject = new BitcoinURIListeningService(args);
    testObject.start();

    Socket client = serverSocket.accept();

    String text;
    try (InputStreamReader reader = new InputStreamReader(client.getInputStream(), Charsets.UTF_8)) {
      text = CharStreams.toString(reader);
    }
    client.close();

    String expectedMessage = BitcoinURIListeningService.MESSAGE_START +"bitcoin:1AhN6rPdrMuKBGFDKR1k9A8SCLYaNgXhty?amount=0.01&label=Please%20donate%20to%20multibit.org"+BitcoinURIListeningService.MESSAGE_END;
    assertThat(text).isEqualTo(expectedMessage);

    // Don't crash the JVM
    CoreEvents.fireShutdownEvent(ShutdownEvent.ShutdownType.SOFT);

    assertThat(testObject.getServerSocket().isPresent()).isFalse();
  }

  @Test
  public void testNotify_AddressOnly() throws Exception {

    try {
      serverSocket = new ServerSocket(
        BitcoinURIListeningService.MULTIBIT_HD_NETWORK_SOCKET,
        10,
        InetAddress.getLoopbackAddress()
      );
    } catch (BindException e) {
      fail("Address already in use - is another version of DigiBit HD already running?");
    }

    String[] args = new String[]{
      RAW_URI_ADDRESS
    };

    testObject = new BitcoinURIListeningService(args);
    testObject.start();

    Socket client = serverSocket.accept();

    String text;
    try (InputStreamReader reader = new InputStreamReader(client.getInputStream(), Charsets.UTF_8)) {
      text = CharStreams.toString(reader);
    }
    client.close();

    String expectedMessage = BitcoinURIListeningService.MESSAGE_START +"bitcoin:1AhN6rPdrMuKBGFDKR1k9A8SCLYaNgXhty"+BitcoinURIListeningService.MESSAGE_END;
    assertThat(text).isEqualTo(expectedMessage);

    // Don't crash the JVM
    CoreEvents.fireShutdownEvent(ShutdownEvent.ShutdownType.SOFT);

    assertThat(testObject.getServerSocket().isPresent()).isFalse();

  }

}
