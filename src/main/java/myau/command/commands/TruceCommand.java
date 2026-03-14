package myau.command.commands;

import myau.Myau;
import myau.command.Command;
import myau.enums.ChatColors;
import myau.util.ChatUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

public class TruceCommand extends Command {
    public TruceCommand() {
        super(new ArrayList<>(Arrays.asList("truce", "t")));
    }

    @Override
    public void runCommand(ArrayList<String> args) {
        if (args.size() >= 2){
        String subCommand = args.get(1).toLowerCase(Locale.ROOT);
        switch (subCommand) {
            case "a":
            case "add":
                if (args.size() < 3) {
                    ChatUtil.sendFormatted(
                            String.format("%sUsage: .%s add <&oname&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT))
                    );
                    return;
                }
                for (String name: args.subList(2, args.size())) {
                    String added = Myau.truceManager.add(name);
                    if (added == null) {
                        ChatUtil.sendFormatted(String.format("%s&o%s&r is already in your truce list&r", Myau.clientName, name));
                    } else {
                        ChatUtil.sendFormatted(String.format("%sAdded &o%s&r to your truce list&r", Myau.clientName, added));
                    }
                }
                return;
            case "r":
            case "remove":
                if (args.size() < 3) {
                    ChatUtil.sendFormatted(
                            String.format("%sUsage: .%s remove <&oname&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT))
                    );
                    return;
                }
                for (String name: args.subList(2, args.size())){
                    String removed = Myau.truceManager.remove(name);
                    if (removed == null) {
                        ChatUtil.sendFormatted(String.format("%s&o%s&r is not in your truce list&r", Myau.clientName, name));
                    } else {
                        ChatUtil.sendFormatted(String.format("%sRemoved &o%s&r from your truce list&r", Myau.clientName, removed));
                    }
                }
                return;
            case "l":
            case "list":
                ArrayList<String> list = Myau.truceManager.getPlayers();
                if (list.isEmpty()) {
                    ChatUtil.sendFormatted(String.format("%sNo Truce&r", Myau.clientName));
                    return;
                }
                ChatUtil.sendFormatted(String.format("%sTruce:&r", Myau.clientName));
                for (String player : list) {
                    ChatUtil.sendRaw(String.format(ChatColors.formatColor("   &o%s&r"), player));
                }
                return;
            case "clear":
                Myau.truceManager.clear();
                ChatUtil.sendFormatted(String.format("%sCleared your truce list&r", Myau.clientName));
                return;
            default:
                if (args.size() == 2) {
                    if (Myau.truceManager.isTruce(args.get(1))) {
                        runCommand(new ArrayList<>(Arrays.asList(args.get(0), "remove", args.get(1))));
                    } else {
                        runCommand(new ArrayList<>(Arrays.asList(args.get(0), "add", args.get(1))));
                    }
                    return;
                }
            }
        }
        ChatUtil.sendFormatted(
                String.format("%sUsage: .%s <&oa(dd)&r/&or(emove)&r/&ol(ist)&r/&oc(lear)&r>&r", Myau.clientName, args.get(0).toLowerCase(Locale.ROOT))
        );
    }

}
