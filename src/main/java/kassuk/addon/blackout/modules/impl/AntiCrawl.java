package kassuk.addon.blackout.modules.impl;

import kassuk.addon.blackout.BlackOut;
import kassuk.addon.blackout.modules.BlackOutModule;

/**
 * @author OLEPOSSU
 */

public class AntiCrawl extends BlackOutModule {
    public AntiCrawl() {
        super(BlackOut.BLACKOUT, "anti-crawl", "Doesn't crawl or sneak when in low space (should be used on 1.12.2).");
    }
}
