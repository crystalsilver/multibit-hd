package org.multibit.hd.ui.controllers;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import org.joda.money.BigMoney;
import org.multibit.hd.core.dto.WalletData;
import org.multibit.hd.core.events.ExchangeRateChangedEvent;
import org.multibit.hd.core.managers.WalletManager;
import org.multibit.hd.core.services.CoreServices;
import org.multibit.hd.core.utils.Satoshis;
import org.multibit.hd.ui.events.controller.AddAlertEvent;
import org.multibit.hd.ui.events.controller.RemoveAlertEvent;
import org.multibit.hd.ui.events.view.ViewEvents;
import org.multibit.hd.ui.models.AlertModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.List;

/**
 * <p>Controller for the header view </p>
 * <ul>
 * <li>Handles interaction between the model and the view</li>
 * </ul>
 */
public class HeaderController {

  private static final Logger log = LoggerFactory.getLogger(HeaderController.class);

  private final List<AlertModel> alertModels = Lists.newArrayList();

  public HeaderController() {

    CoreServices.uiEventBus.register(this);

  }

  /**
   * <p>Called when the balance changes</p>
   *
   * @param event The exchange rate change event
   */
  @Subscribe
  public void onBalanceChanged(ExchangeRateChangedEvent event) {

    // Build the exchange string
    // TODO Link to a real balance and remove BigDecimal
    BigInteger satoshis;

    Optional<WalletData> currentWalletData = WalletManager.INSTANCE.getCurrentWalletData();
    if (currentWalletData.isPresent()) {
      // Use the real wallet data
      satoshis = currentWalletData.get().getWallet().getBalance();
    } else {
      // Unknown at this time
      satoshis = BigInteger.ZERO;
    }
    BigMoney localBalance = Satoshis.toLocalAmount(satoshis, event.getRate());

    // Post the event
    ViewEvents.fireBalanceChangedEvent(
      satoshis,
      localBalance,
      event.getRateProvider()
    );

  }

  /**
   * <p>Handles the representation of the balance based on the current configuration</p>
   *
   * @param event The balance change event
   */
  @Subscribe
  public synchronized void onAddAlertEvent(AddAlertEvent event) {

    // Add this to the list
    alertModels.add(event.getAlertModel());

    // Adjust the models to reflect the new M of N values
    updateRemaining();

    // The alert structure has changed so inform the view
    ViewEvents.fireAlertAddedEvent(alertModels.get(0));

  }

  /**
   * <p>Handles the representation of the balance based on the current configuration</p>
   *
   * @param event The balance change event
   */
  @Subscribe
  public synchronized void onRemoveAlertEvent(RemoveAlertEvent event) {

    // Remove the topmost alert model
    alertModels.remove(0);

    if (!alertModels.isEmpty()) {

      // Adjust the models to reflect the new M of N values
      updateRemaining();

      // The alert structure has changed so inform the view
      ViewEvents.fireAlertAddedEvent(alertModels.get(0));

    } else {

      // Use an empty event to signal that the event should be hidden
      ViewEvents.fireAlertRemovedEvent();
    }

  }

  /**
   * <p>Updates the "remaining" values for alert models</p>
   */
  private void updateRemaining() {

    Preconditions.checkNotNull(alertModels,"'alertModels' must be present");

    // Update the "remaining" based on the position in the list
    for (int i = 0; i < alertModels.size(); i++) {
      AlertModel alertModel = alertModels.get(i);

      Preconditions.checkNotNull(alertModel,"'alertModel' must be present");

      alertModel.setRemaining(alertModels.size()-1);
    }

  }

}
