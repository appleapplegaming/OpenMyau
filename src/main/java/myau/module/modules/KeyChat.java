package myau.module.modules;

import myau.module.Module;
import myau.property.properties.TextProperty;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;

public class KeyChat extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final TextProperty text = new TextProperty("message", "Hello World!");

    public KeyChat() {
        super("KeyChat", false);
    }

    @Override
    public void onEnabled() {
        ChatUtil.sendMessage(text.getValue());

        setEnabled(false);
    }

}
