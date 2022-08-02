package com.waisapps.lichessscheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class PlayerFileManager {
    public static String getPlayers(File filesDir, String token) throws IOException {
        File playersFile = new File(String.format("%s/data/%s/players", filesDir, token));
        FileInputStream fis = new FileInputStream(playersFile);
        byte[] data = new byte[(int) playersFile.length()];
        if (fis.read(data) == -1) return "";
        fis.close();
        return new String(data, StandardCharsets.UTF_8);
    }

    public static void writePlayersToFile(File filesDir, String token, String playersJSON) throws IOException {
        File playersFile = new File(String.format("%s/data/%s/players", filesDir, token));
        FileOutputStream playersFileStream = new FileOutputStream(playersFile);
        playersFileStream.write(playersJSON.getBytes(StandardCharsets.UTF_8));
        playersFileStream.close();
    }
}
