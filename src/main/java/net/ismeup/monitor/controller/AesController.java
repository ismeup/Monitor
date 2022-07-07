package net.ismeup.monitor.controller;

import net.ismeup.monitor.exceptions.AesException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class AesController {
    private String key;
    private boolean isCiphersOk = false;
    private Cipher encryptAesCipher;
    private Cipher decryptAesCipher;

    private static Object encryptLock = new Object();
    private static Object decryptLock = new Object();

    public AesController(String key) throws AesException {
        this.key = key;
        initCiphers();
    }

    private void initCiphers() {
            MessageDigest sha = null;
            byte[] key = new byte[0];
            try {
                key = this.key.getBytes("UTF-8");
                sha = MessageDigest.getInstance("SHA-1");
                key = sha.digest(key);
                key = Arrays.copyOf(key, 16);
                SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
                encryptAesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                encryptAesCipher.init(Cipher.ENCRYPT_MODE, secretKey);
                decryptAesCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
                decryptAesCipher.init(Cipher.DECRYPT_MODE, secretKey);
                isCiphersOk = true;
            } catch (UnsupportedEncodingException e) {

            } catch (NoSuchPaddingException e) {

            } catch (NoSuchAlgorithmException e) {

            } catch (InvalidKeyException e) {

            }
    }

    public byte[] encrypt(byte[] b) throws AesException{
        byte[] result = null;
        if (isCiphersOk) {
            try {
                synchronized (encryptLock) {
                    result = encryptAesCipher.doFinal(b);
                }
            } catch (Exception e) {
                throw new AesException();
            }
        } else {
            throw new AesException();
        }
        return result;
    }

    public byte[] decrypt(byte[] b) throws AesException{
        byte[] result = null;
        if (isCiphersOk) {
            try {
                synchronized (decryptLock) {
                    result = decryptAesCipher.doFinal(b);
                }
            } catch (Exception e) {
                throw new AesException();
            }
        } else {
            throw new AesException();
        }
        return result;
    }
}
