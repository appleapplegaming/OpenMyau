package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.events.UpdateEvent; // rename if your project uses a different tick/update event
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.property.properties.TextProperty;
import myau.util.ChatUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuickMaths extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // User-editable: wait this long AFTER detecting the math before sending the answer
    public final FloatProperty delay = new FloatProperty("delay", 1.0f, 0.0f, 10.0f);
    public final TextProperty prefix = new TextProperty("prefix", "");

    // Hidden/internal cooldown: one answer max every 30 seconds
    private static final long ANSWER_COOLDOWN_MS = 30_000L;

    private static final Pattern QUICK_MATHS_PATTERN = Pattern.compile(
            "quick\\s*maths\\s*!?\\s*solve\\s*:\\s*(.+)",
            Pattern.CASE_INSENSITIVE
    );

    private long lastAnswerTime = 0L;
    private long scheduledSendTime = -1L;
    private String pendingAnswer = null;

    public QuickMaths() {
        super("QuickMaths", false);
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (!this.isEnabled()) return;
        if (event.getType() != EventType.RECEIVE) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;

        Packet<?> packet = event.getPacket();
        if (!(packet instanceof S02PacketChat)) return;

        String msg = ((S02PacketChat) packet).getChatComponent().getUnformattedText();
        if (msg == null || msg.trim().isEmpty()) return;

        String expression = extractExpression(msg);
        if (expression == null || expression.isEmpty()) return;

        long now = System.currentTimeMillis();

        // Hidden cooldown: ignore new QuickMaths events if we answered within the last 30s
        if (now - lastAnswerTime < ANSWER_COOLDOWN_MS) {
            return;
        }

        try {
            double result = evaluate(expression);
            pendingAnswer = formatAnswer(result);
            scheduledSendTime = now + (long) (delay.getValue() * 1000.0f);
        } catch (Exception ignored) {
            pendingAnswer = null;
            scheduledSendTime = -1L;
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) { // rename if needed
        if (!this.isEnabled()) return;
        if (mc.thePlayer == null || mc.theWorld == null) return;
        if (pendingAnswer == null || scheduledSendTime < 0L) return;

        long now = System.currentTimeMillis();
        if (now >= scheduledSendTime) {
            String answer = prefix.getValue() + " " + pendingAnswer;
            ChatUtil.sendMessage(answer);
            lastAnswerTime = now;
            pendingAnswer = null;
            scheduledSendTime = -1L;
        }
    }

    private String extractExpression(String message) {
        Matcher matcher = QUICK_MATHS_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        String expr = matcher.group(1);
        if (expr == null) return null;

        expr = expr.replaceAll("§.", "");
        expr = expr.trim().replace(" ", "");
        expr = expr.replace('x', '*').replace('X', '*').replace('×', '*');
        expr = expr.replaceAll("[^0-9+\\-*/().]", "");

        if (expr.isEmpty()) return null;

        expr = expr.replaceAll("\\)\\(", ")*(");
        expr = expr.replaceAll("(\\d)\\(", "$1*(");
        expr = expr.replaceAll("\\)(\\d)", ")*$1");

        return expr;
    }

    private String formatAnswer(double value) {
        if (Math.abs(value - Math.rint(value)) < 1.0E-9) {
            return String.valueOf((long) Math.rint(value));
        }

        BigDecimal bd = BigDecimal.valueOf(value)
                .setScale(8, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        return bd.toPlainString();
    }

    private double evaluate(String expression) {
        return new Parser(expression).parse();
    }

    private static class Parser {
        private final String s;
        private int pos = -1;
        private int ch;

        Parser(String s) {
            this.s = s;
        }

        double parse() {
            nextChar();
            double x = parseExpression();
            if (pos < s.length()) {
                throw new RuntimeException("Unexpected: " + (char) ch);
            }
            return x;
        }

        private void nextChar() {
            ch = (++pos < s.length()) ? s.charAt(pos) : -1;
        }

        private boolean eat(int charToEat) {
            while (ch == ' ') nextChar();
            if (ch == charToEat) {
                nextChar();
                return true;
            }
            return false;
        }

        private double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) x += parseTerm();
                else if (eat('-')) x -= parseTerm();
                else return x;
            }
        }

        private double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if (eat('*')) x *= parseFactor();
                else if (eat('/')) x /= parseFactor();
                else return x;
            }
        }

        private double parseFactor() {
            if (eat('+')) return parseFactor();
            if (eat('-')) return -parseFactor();

            double x;
            int startPos = this.pos;

            if (eat('(')) {
                x = parseExpression();
                if (!eat(')')) {
                    throw new RuntimeException("Missing ')'");
                }
            } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                x = Double.parseDouble(s.substring(startPos, this.pos));
            } else {
                throw new RuntimeException("Unexpected: " + (char) ch);
            }

            return x;
        }
    }
}