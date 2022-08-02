package com.waisapps.lichessscheduler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TournamentFileManager {
    public static String getTournamentJSON(File filesDir, String token, String tnrId) throws IOException {
        File tnrFile = new File(String.format("%s/data/%s/tournaments/%s",
                filesDir, token, tnrId));
        FileInputStream fis = new FileInputStream(tnrFile);
        byte[] data = new byte[(int) tnrFile.length()];
        if (fis.read(data) == -1) return "";
        fis.close();
        return new String(data, StandardCharsets.UTF_8);
    }

    public static void writeTournamentToFile(File filesDir, String token, String tnrId, String tnrJSON) throws IOException {
        File dataDir = new File(filesDir, "data");
        if (!dataDir.exists()) {
            dataDir.mkdir();
        }
        File tokenDir = new File(dataDir, token);
        if (!tokenDir.exists()) {
            tokenDir.mkdir();
        }
        File tnrDir = new File(tokenDir, "tournaments");
        if (!tnrDir.exists()) {
            tnrDir.mkdir();
        }
        File tnrFile = new File(tnrDir, tnrId);
        FileOutputStream tnrFileStream = new FileOutputStream(tnrFile);
        tnrFileStream.write(tnrJSON.getBytes(StandardCharsets.UTF_8));
        tnrFileStream.close();
    }

    public static File[] getTournamentsByToken(File filesDir, String token) {
        File tnrDir = new File(String.format("%s/data/%s/tournaments",
                filesDir, token));
        return tnrDir.listFiles();
    }
}
