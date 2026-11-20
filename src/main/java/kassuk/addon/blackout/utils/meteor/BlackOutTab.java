package kassuk.addon.blackout.utils.meteor;

import kassuk.addon.blackout.utils.meteor.GlobalSettingsConfig.Section;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.tabs.Tab;
import meteordevelopment.meteorclient.gui.tabs.TabScreen;
import meteordevelopment.meteorclient.gui.tabs.WindowTabScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WContainer;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import net.minecraft.client.gui.screen.Screen;

import java.util.LinkedHashMap;
import java.util.Map;

public final class BlackOutTab extends Tab {
    public BlackOutTab() {
        super("BlackOut");
    }

    @Override
    public TabScreen createScreen(GuiTheme theme) {
        return new BlackOutScreen(theme, this);
    }

    @Override
    public boolean isScreen(Screen screen) {
        return screen instanceof BlackOutScreen;
    }

    public static final class BlackOutScreen extends WindowTabScreen {
        private final Map<Settings, WContainer> containers = new LinkedHashMap<>();

        public BlackOutScreen(GuiTheme theme, Tab tab) {
            super(theme, tab);
            GlobalSettingsConfig.get().sections.forEach(section -> section.settings().onActivated());
            onClosed(() -> GlobalSettingsConfig.get().save());
        }

        @Override
        public void initWidgets() {
            containers.clear();
            for (Section section : GlobalSettingsConfig.get().sections) {
                String title = section.name().equals("Rotate") ? "Rotation" : section.name();
                // Share the actual settings while presenting a single top-level group.
                // Keep the persisted groups intact so existing configurations still load.
                Settings settings = new Settings();
                SettingGroup main = settings.createGroup(title, true);
                for (SettingGroup group : section.settings()) {
                    group.forEach(setting -> main.add(setting));
                }
                WContainer container = add(theme.verticalList()).expandX().widget();
                container.add(theme.settings(settings)).expandX();
                containers.put(settings, container);
            }
        }

        @Override
        public void tick() {
            super.tick();
            containers.forEach((settings, container) -> settings.tick(container, theme));
        }
    }
}
