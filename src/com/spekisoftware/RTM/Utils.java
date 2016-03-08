package com.spekisoftware.RTM;

import java.security.MessageDigest;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Utils
{
    private static final byte[] ENCRYPTION_KEY = { 2, 18, -121, -93, -69, -103, -108, -30, 96, 62, -62, -111, 91, -48,
            -54, -125                         };

    public static String EncryptString(String plainText)
    {

        try
        {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY, "AES");

            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes());

            return Base64.encodeToString(cipherBytes, false);
        }
        catch (Exception ex)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, "EncryptString", String.format("Caught exception while trying to encrypt: %s", ex.toString()));
        }

        return null;
    }

    public static String DecryptString(String cipherText)
    {
        try
        {
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY, "AES");

            Cipher cipher = Cipher.getInstance("AES");

            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] cipherBytes = Base64.decodeFast(cipherText);

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes);
        }
        catch (Exception ex)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, "DecryptString", String.format("Caught exception while trying to decrypt: %s", ex.toString()));
        }

        return null;

    }
    
    private static final String HEXES = "0123456789abcdef";

    public static String BytesToHex( byte [] raw ) {
        if ( raw == null ) {
          return null;
        }
        final StringBuilder hex = new StringBuilder( 2 * raw.length );
        for ( final byte b : raw ) {
          hex.append(HEXES.charAt((b & 0xF0) >> 4))
             .append(HEXES.charAt((b & 0x0F)));
        }
        return hex.toString();
    }
    
    public static String GetMD5Hash(String plainText)
    {
        try
        {
            byte[] plainTextBytes = plainText.getBytes();

            MessageDigest md = MessageDigest.getInstance("MD5");
            return BytesToHex(md.digest(plainTextBytes));
        }
        catch (Exception ex)
        {
            Logger.Log(Logger.LOG_LEVEL_ERROR, "GetMD5Hash", String.format("Caught exception while trying to hash: %s", ex.toString()));
        }

        return null;
    }

    public static String ReplaceTokens(String tokenString, Map<String, String> tokens)
    {
        Pattern pattern = Pattern.compile("(\\{\\{.+?\\}\\})");
        Matcher matcher = pattern.matcher(tokenString);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find())
        {
            String replacement = tokens.get(matcher.group(1));
            if (replacement != null)
            {
                matcher.appendReplacement(buffer, "");
                buffer.append(replacement);
            }
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
