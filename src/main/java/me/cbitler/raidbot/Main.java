package me.cbitler.raidbot;

import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import javax.security.auth.login.LoginException;

import me.cbitler.raidbot.utility.EnvVariables;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Start the program, read the token, and start the bot
 * 
 * @author Christopher Bitler
 */
public class Main {
    public static void main(String[] args) throws LoginException, InterruptedException, RateLimitedException {
        String token = null;
        try {
            token = readToken();
        } catch (IOException e) {
            System.out.println("Specify Discord Bot Token in file 'token'");
            // System.exit(1);
        }
        if (token == null) {
            try {
                token = System.getenv("DISC_TOKEN");
                System.out.println("token: " + token);
            } catch (Exception e) {
                System.out.println("env var not working");
                System.exit(1);
            }
        }

        JDA jda = new JDABuilder(AccountType.BOT).setToken(token).buildBlocking();
        new RaidBot(jda);
    }

    /**
     * Read the token from the token file
     * 
     * @return The token text
     * @throws IOException
     */
    private static String readToken() throws IOException {
        // get token file from jar dir instead of execution dir
        URI tokenPath;
        try {
            System.out.println(Main.class.getProtectionDomain().getCodeSource().getLocation().toString());

            tokenPath = Main.class.getProtectionDomain().getCodeSource().getLocation().toURI().resolve("token");
        } catch (URISyntaxException e) {
            throw new IOException();
        }
        File tokenFile = new File(tokenPath);
        // if token file does not exist in jar dir, try loading it from execution dir
        if (!tokenFile.exists())
            tokenFile = new File("token");
        BufferedReader br = new BufferedReader(new FileReader(tokenFile));
        String outer = br.readLine();
        br.close();
        return outer;
    }
}
