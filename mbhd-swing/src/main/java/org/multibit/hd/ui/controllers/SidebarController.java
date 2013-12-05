package org.multibit.hd.ui.controllers;

import org.multibit.hd.core.services.CoreServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Controller for the sidebar view (tree view of wallets)</p>
 * <ul>
 * <li>Handles interaction between the model and the view</li>
 * </ul>
 */
public class SidebarController {

  private static final Logger log = LoggerFactory.getLogger(SidebarController.class);

  public SidebarController() {

    CoreServices.uiEventBus.register(this);

  }

}