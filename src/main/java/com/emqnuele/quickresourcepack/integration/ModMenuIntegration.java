package com.emqnuele.quickresourcepack.integration;

import com.emqnuele.quickresourcepack.ui.HubScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HubScreen::new;
    }
}
