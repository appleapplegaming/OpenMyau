package myau.module.modules;

import myau.event.EventTarget;
import myau.event.types.EventType;
import myau.events.PacketEvent;
import myau.module.Module;
import myau.property.properties.FloatProperty;
import myau.util.ChatUtil;
import myau.util.TimerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S02PacketChat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QuickMaths extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();

    // small delay so you don't instantly answer every event
    public final FloatProperty delay = new FloatProperty("delay", 0.2f, 0.0f, 5.0f);

    private final TimerUtil timer = new TimerUtil();

    // Matches:
    // QUICK MATHS! 2+6
    // QUICK MATHS! (4-5)(5x5)
    // and similar
    private static final Pattern QUICK_MATHS_PATTERN = Pattern.compile(
            "quick\\s*maths\\s*!?\\s*[:\\-»>]*\\s*(.+)",
            Pattern.CASE_INSENSITIVE
    );

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

        // optional delay / anti-spam
        if (delay.getValue() > 0.0F && !timer.hasTimeElapsed((long) (delay.getValue() * 1000.0F))) {
            return;
        }

        try {
            double result = evaluate(expression);
            String answer = formatAnswer(result);

            timer.reset();
            ChatUtil.sendMessage(answer);
        } catch (Exception ignored) {
            // invalid format -> do nothing
        }
    }

    private String extractExpression(String message) {
        Matcher matcher = QUICK_MATHS_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        String expr = matcher.group(1);
        if (expr == null) return null;

        // strip Minecraft formatting if any leaked through
        expr = expr.replaceAll("§.", "");

        // normalize spaces
        expr = expr.trim().replace(" ", "");

        // normalize multiplication symbols
        expr = expr.replace('x', '*').replace('X', '*').replace('×', '*');

        // keep only math-safe characters
        expr = expr.replaceAll("[^0-9+\\-*/().]", "");

        if (expr.isEmpty()) return null;

        // support implicit multiplication:
        // (4-5)(5*5) -> (4-5)*(5*5)
        // 2(3+4) -> 2*(3+4)
        // (2+3)4 -> (2+3)*4
        expr = expr.replaceAll("\\)\\(", ")*(");
        expr = expr.replaceAll("(\\d)\\(", "$1*(");
        expr = expr.replaceAll("\\)(\\d)", ")*$1");

        return expr;
    }

    private String formatAnswer(double value) {
        // if whole number, send as integer
        if (Math.abs(value - Math.rint(value)) < 1.0E-9) {
            return String.valueOf((long) Math.rint(value));
        }

        // otherwise trim trailing zeros nicely
        BigDecimal bd = BigDecimal.valueOf(value).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
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

        // expression = term | expression `+` term | expression `-` term
        private double parseExpression() {
            double x = parseTerm();
            for (;;) {
                if (eat('+')) {
                    x += parseTerm();
                } else if (eat('-')) {
                    x -= parseTerm();
                } else {
                    return x;
                }
            }
        }

        // term = factor | term `*` factor | term `/` factor
        private double parseTerm() {
            double x = parseFactor();
            for (;;) {
                if (eat('*')) {
                    x *= parseFactor();
                } else if (eat('/')) {
                    x /= parseFactor();
                } else {
                    return x;
                }
            }
        }

        // factor = `+` factor | `-` factor | number | `(` expression `)`
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