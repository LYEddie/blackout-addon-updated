package kassuk.addon.blackout.managers;

import kassuk.addon.blackout.managers.impl.HoldingManager;
import kassuk.addon.blackout.managers.impl.OnGroundManager;
import kassuk.addon.blackout.managers.impl.PingSpoofManager;
import kassuk.addon.blackout.managers.impl.RotationManager;

/**
 * @author OLEPOSSU
 */

public class Managers {

    public static final HoldingManager HOLDING = new HoldingManager();
    public static final PingSpoofManager PING_SPOOF = new PingSpoofManager();
    public static final OnGroundManager ON_GROUND = new OnGroundManager();
    public static final RotationManager ROTATION = new RotationManager();

}
