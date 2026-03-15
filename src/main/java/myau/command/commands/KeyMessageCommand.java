package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.management.KeyMessageEntry;
import myau.util.ChatUtil;
import myau.util.KeyBindUtil;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class KeyMessageCommand extends Command {
    public KeyMessageCommand() {
        super(new ArrayList<>(Arrays.asList("keymessage", "km")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() < 2) {
            sendUsage(args.get(0));
            return;
        }

        String sub = args.get(1).toLowerCase(Locale.ROOT);

        if (sub.equals("list") || sub.equals("l")) {
            listBindings();
        } else if (sub.equals("clear")) {
            Myau.keyMessageManager.entries.clear();
            Myau.keyMessageManager.save();
            ChatUtil.sendFormatted(String.format("%sCleared all key message binds&r", Myau.clientName));
        } else if (sub.equals("remove") || sub.equals("r")) {
            if (args.size() < 3) {
                ChatUtil.sendFormatted(String.format("%sUsage: .%s remove <&oindex&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT)));
                return;
            }
            try {
                int index = Integer.parseInt(args.get(2)) - 1;
                if (index < 0 || index >= Myau.keyMessageManager.entries.size()) {
                    ChatUtil.sendFormatted(String.format("%sIndex out of range&r", Myau.clientName));
                    return;
                }
                KeyMessageEntry removed = Myau.keyMessageManager.entries.remove(index);
                Myau.keyMessageManager.save();
                ChatUtil.sendFormatted(String.format("%sRemoved bind &l[%s]&r &o%s&r", Myau.clientName, KeyBindUtil.getKeyName(removed.key), removed.message));
            } catch (NumberFormatException e) {
                ChatUtil.sendFormatted(String.format("%sInvalid index&r", Myau.clientName));
            }
        } else if (sub.equals("add") || sub.equals("a")) {
            if (args.size() < 4) {
                ChatUtil.sendFormatted(String.format("%sUsage: .%s add <&okey&r> <&omessage&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT)));
                return;
            }
            String keyInput = args.get(2).toUpperCase(Locale.ROOT);
            String message = String.join(" ", args.subList(3, args.size()));
            int keyIndex = parseKey(keyInput);
            if (keyIndex == Integer.MIN_VALUE) {
                ChatUtil.sendFormatted(String.format("%sUnknown key: &o%s&r", Myau.clientName, args.get(2)));
                return;
            }
            Myau.keyMessageManager.entries.add(new KeyMessageEntry(keyIndex, message));
            Myau.keyMessageManager.save();
            ChatUtil.sendFormatted(String.format("%sBound &l[%s]&r to message &o%s&r", Myau.clientName, KeyBindUtil.getKeyName(keyIndex), message));
        } else {
            sendUsage(args.get(0));
        }
    }

    private void listBindings() {
        if (Myau.keyMessageManager.entries.isEmpty()) {
            ChatUtil.sendFormatted(String.format("%sNo key message binds&r", Myau.clientName));
            return;
        }
        ChatUtil.sendFormatted(String.format("%sKey message binds:&r", Myau.clientName));
        for (int i = 0; i < Myau.keyMessageManager.entries.size(); i++) {
            KeyMessageEntry entry = Myau.keyMessageManager.entries.get(i);
            ChatUtil.sendFormatted(String.format("&7%d. &l[%s]&r &o%s&r", i + 1, KeyBindUtil.getKeyName(entry.key), entry.message));
        }
    }

    private void sendUsage(String commandName) {
        String cmd = commandName.toLowerCase(Locale.ROOT);
        ChatUtil.sendFormatted(String.format(
                "%sUsage: .%s add <&okey&r> <&omessage&r>&r | .%s remove <&oindex&r>&r | .%s list&r | .%s clear&r",
                Myau.clientName, cmd, cmd, cmd, cmd
        ));
    }

    private int parseKey(String keyInput) {
        if (keyInput.equalsIgnoreCase("NONE") || keyInput.equalsIgnoreCase("NULL") || keyInput.equalsIgnoreCase("0")) {
            return 0;
        }
        int keyIndex = Keyboard.getKeyIndex(keyInput);
        if (keyIndex != 0) {
            return keyIndex;
        }
        int buttonIndex = getMouseButtonIndex(keyInput);
        if (buttonIndex != -1) {
            return buttonIndex - 100;
        }
        return Integer.MIN_VALUE;
    }

    private int getMouseButtonIndex(String buttonName) {
        if (buttonName.startsWith("MOUSE")) {
            try {
                String numStr = buttonName.substring(5);
                int buttonNum = Integer.parseInt(numStr);
                if (buttonNum >= 0 && buttonNum < Mouse.getButtonCount()) {
                    return buttonNum;
                }
            } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                // ignore
            }
        }
        int buttonIndex = Mouse.getButtonIndex(buttonName);
        if (buttonIndex != -1) {
            return buttonIndex;
        }
        switch (buttonName) {
            case "LBUTTON":
            case "LMB":
            case "LEFTCLICK":
                return 0;
            case "RBUTTON":
            case "RMB":
            case "RIGHTCLICK":
                return 1;
            case "MBUTTON":
            case "MMB":
            case "MIDDLECLICK":
            case "SCROLLCLICK":
                return 2;
            case "MOUSE3":
            case "XBUTTON1":
            case "SIDEBUTTON1":
            case "BOTTOMSIDE":
                return 3;
            case "MOUSE4":
            case "XBUTTON2":
            case "SIDEBUTTON2":
            case "TOPSIDE":
                return 4;
            case "MOUSE5":
                return 5;
            case "MOUSE6":
                return 6;
            case "MOUSE7":
                return 7;
            default:
                return -1;
        }
    }
}
