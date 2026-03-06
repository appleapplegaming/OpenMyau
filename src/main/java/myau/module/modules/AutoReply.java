package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.TextProperty;
import myau.util.ChatUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;

public class AutoReply extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    public final TextProperty trigger = new TextProperty("trigger", "marko");
    public final TextProperty reply = new TextProperty("reply", "polo");
    public final FloatProperty cooldown = new FloatProperty("cooldown", 2.0f, 0.5f, 30.0f);

    private final TimerUtil timer = new TimerUtil();

    public AutoReply() {
        super("AutoReply", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.RECEIVE) return;   // PacketEvent is typed (SEND/RECEIVE) :contentReference[oaicite:2]{index=2}
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Packet<?> p = event.getPacket();
        if (!(p instanceof S02PacketChat)) return;

        S02PacketChat chat = (S02PacketChat) p;

        // Get the plain text (no formatting codes)
        String msg = chat.getChatComponent().getUnformattedText();
        if (msg == null) return;

        // Basic anti-spam / anti-loop: cooldown
        if (cooldown.getValue() > 0.0F && !timer.hasTimeElapsed((long)(cooldown.getValue() * 1000.0F))) {
            return;
        }

        // Match "Marko" anywhere, case-insensitive
        String trig = trigger.getValue();
        if (trig == null || trig.trim().isEmpty()) return;

        if (msg.toLowerCase().contains(trig.toLowerCase())) {
            timer.reset();
            ChatUtil.sendMessage(reply.getValue());
        }
    }
}
