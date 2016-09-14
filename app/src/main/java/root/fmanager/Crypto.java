package root.fmanager;

import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

@SuppressWarnings("ResultOfMethodCallIgnored")
class Crypto {

    private static final String PROVIDER = "BC";
    //private static final int SALT_LENGTH = 20;
    //private static final int IV_LENGTH = 16;
    private static final int PBE_ITERATION_COUNT = 5000;
    private static final int PBE_KEY_LENGTH = 256;

    //private static final String RANDOM_ALGORITHM = "SHA1PRNG";
    //private static final String HASH_ALGORITHM = "SHA-512";
    private static final String PBE_ALGORITHM = "PBKDF2withHmacSHA1and8BIT";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String SECRET_KEY_ALGORITHM = "AES";

    static boolean encrypt (File file, String password) {
        String filePath = file.getAbsolutePath();
        String tmpFilePath = FManagerActivity.appDataDir.getAbsolutePath() + File.separator + file.getName();

        File tmpFile = new File(tmpFilePath);

        FileInputStream fis = null;
        CipherOutputStream cos = null;
        byte[] buff;
        byte[] iv = new byte[]{};
        int i;

        try {
            // Here you read the cleartext.
            fis = new FileInputStream(file);
            // This stream write the encrypted text. This stream will be wrapped by another stream.

            byte[] salt = file.getName().getBytes("ISO_8859_1");

            Log.d("ENC_FILE_BEFORE", new String(IOUtils.toByteArray(new FileInputStream(file)), "ISO_8859_1"));

            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBE_ITERATION_COUNT, PBE_KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), SECRET_KEY_ALGORITHM);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, secret);
            iv = cipher.getIV();

            Log.d("ENC_IV", new String(iv, "ISO_8859_1"));

            // Wrap the output stream
            cos = new CipherOutputStream(new FileOutputStream(tmpFile), cipher);
            // Write bytes
            buff = new byte[1024];
            while((i = fis.read(buff)) != -1) {
                cos.write(buff, 0, i);
            }
            file.delete();
            Utils.moveFiles(new File[]{tmpFile}, file.getParent());
            file = new File(filePath);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (cos != null) {
                    cos.flush();
                    cos.close();
                }
                if (fis != null)
                    fis.close();
            } catch (IOException ignored) {}
        }

        //Append iv to the encrypted file
        FileOutputStream fos = null;
        ByteArrayInputStream bais = null;
        try {
            byte[] title = ("iv:" + file.length() + ":").getBytes("ISO_8859_1");
            byte[] finalIv = new byte[title.length + iv.length];
            System.arraycopy(title, 0, finalIv, 0, title.length);
            System.arraycopy(iv, 0, finalIv, title.length, iv.length);

            Log.d("ENC_FINALIV", new String(finalIv, "ISO_8859_1"));

            fos = new FileOutputStream(file, true);
            fos.write('\n');
            bais = new ByteArrayInputStream(finalIv);
            buff = new byte[1024];
            while ((i = bais.read(buff)) != -1) {
                fos.write(buff, 0, i);
            }
            Log.d("ENC_FILE_AFTER", new String(IOUtils.toByteArray(new FileInputStream(file)), "ISO_8859_1"));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            try {
                if (fos != null) {
                    fos.flush();
                    fos.close();
                }
                if (bais != null) {
                    bais.close();
                }
            } catch (IOException ignored) {}
        }
        return true;
    }

    static int decrypt (File file, String password) {
        File tmpFile = new File(FManagerActivity.appDataDir.getAbsolutePath() + File.separator + file.getName());
        try {
            Log.d("DEC_FILE_BEFORE", new String(IOUtils.toByteArray(new FileInputStream(file)), "ISO_8859_1"));
            Utils.moveFiles(new File[]{file}, tmpFile.getParent());
            Log.d("SIZEBEFORE", ""+tmpFile.length());

            byte[] salt = tmpFile.getName().getBytes(StandardCharsets.UTF_8);

            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBE_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBE_ITERATION_COUNT, PBE_KEY_LENGTH);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKey secret = new SecretKeySpec(tmp.getEncoded(), SECRET_KEY_ALGORITHM);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM, PROVIDER);

            String lastLine = Utils.lastLineOfFile(tmpFile);
            assert lastLine != null;
            Log.d("LL", lastLine);
            int origFileLength = Integer.parseInt(lastLine.substring(lastLine.indexOf(':') + 1, lastLine.lastIndexOf(':')));
            byte[] iv = lastLine.substring(lastLine.lastIndexOf(':') + 1).getBytes("ISO_8859_1");
            Log.d("IV", new String(iv, "ISO_8859_1"));
            Log.d("LEN", String.valueOf(origFileLength));

            RandomAccessFile fileWOIv = new RandomAccessFile(tmpFile, "rws");
            fileWOIv.setLength(origFileLength);
            Log.d("SIZEAFTER", ""+tmpFile.length());

            cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));

            CipherInputStream cis = new CipherInputStream(new FileInputStream(tmpFile), cipher);
            FileOutputStream fos = new FileOutputStream(file);

            int i;
            byte[] buff = new byte[1024];
            while((i = cis.read(buff)) != -1) {
                fos.write(buff, 0, i);
            }
            fos.flush();
            fos.close();
            cis.close();
            tmpFile.delete();
            Log.d("DEC_FILE_AFTER", new String(IOUtils.toByteArray(new FileInputStream(file)), "ISO_8859_1"));
        } catch (Exception e) {
            e.printStackTrace();
            file.delete();
            Utils.moveFiles(new File[]{tmpFile}, file.getParent());
            try {
                Log.d("DEC_FILE_AFTER", new String(IOUtils.toByteArray(new FileInputStream(file)), "ISO_8859_1"));
            } catch (IOException ignored) {}
            return -1;
        }
        return 1;
    }
}
