package myau.management;

import myau.enums.ChatColors;

import java.awt.*;
import java.io.File;

public class TruceManager extends PlayerFileManager {
    public TruceManager() {
        super(new File("./config/Myau/", "truce.txt"), new Color(ChatColors.AQUA.toAwtColor()));
    }
}
