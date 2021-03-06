package jaygoo.library.m3u8downloader.utils;

import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * ================================================
 * 作    者：JayGoo
 * 版    本：
 * 创建日期：2017/11/23
 * 描    述: AES-128 加密解密工具类
 * ================================================
 */
public class AES128Utils {

    public final static String ENCODING = "UTF-8";
    public static byte[] keyBytes = new byte[16];
    /**将二进制转换成16进制
     * @param buf
     * @return
     */
    public static String parseByte2HexStr(byte buf[]) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < buf.length; i++) {
            String hex = Integer.toHexString(buf[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            sb.append(hex.toUpperCase());
        }
        return sb.toString();
    }

    /**将16进制转换为二进制
     * @param hexStr
     * @return
     */
    public static byte[] parseHexStr2Byte(String hexStr) {
        if (hexStr.length() < 1)
            return null;
        byte[] result = new byte[hexStr.length()/2];
        for (int i = 0;i< hexStr.length()/2; i++) {
            int high = Integer.parseInt(hexStr.substring(i*2, i*2+1), 16);
            int low = Integer.parseInt(hexStr.substring(i*2+1, i*2+2), 16);
            result[i] = (byte) (high * 16 + low);
        }
        return result;
    }

    /**
     * 生成密钥
     * 自动生成base64 编码后的AES128位密钥
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public static String getAESKey() throws Exception {
        KeyGenerator kg = KeyGenerator.getInstance("AES");
        kg.init(128);
        SecretKey sk = kg.generateKey();
        byte[] b = sk.getEncoded();
        return parseByte2HexStr(b);
    }

    /**
     * AES 加密
     * @param base64Key   base64编码后的 AES key
     * @param text  待加密的字符串
     * @return 加密后的byte[] 数组
     * @throws Exception
     */
    public static byte[] getAESEncode(String base64Key, String text) throws Exception{
        return getAESEncode(base64Key, text.getBytes());
    }

    /**
     * AES 加密
     * @param base64Key   base64编码后的 AES key
     * @param bytes  待加密的bytes
     * @return 加密后的byte[] 数组
     * @throws Exception
     */
    public static byte[] getAESEncode(String base64Key, byte[] bytes) throws Exception{
        if (base64Key == null)return bytes;
        byte[] key = parseHexStr2Byte(base64Key);
        SecretKeySpec sKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, sKeySpec);
        return cipher.doFinal(bytes);
    }

    /**
     * AES解密
     * @param base64Key   base64编码后的 AES key
     * @param text  待解密的字符串
     * @return 解密后的byte[] 数组
     * @throws Exception
     */
    public static byte[] getAESDecode(String base64Key, String text) throws Exception{
        return getAESDecode(base64Key, text.getBytes());
    }

    /**
     * AES解密
     * @param base64Key   base64编码后的 AES key
     * @param bytes  待解密的字符串
     * @return 解密后的byte[] 数组
     * @throws Exception
     */
    public static byte[] getAESDecode(String base64Key, byte[] bytes) throws Exception{
        if (base64Key == null)return bytes;
        byte[] key = parseHexStr2Byte(base64Key);
        SecretKeySpec sKeySpec = new SecretKeySpec(key, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, sKeySpec);
        return cipher.doFinal(bytes);
    }

    /**
     * 解密ts
     *
     * @param sSrc   ts文件字节数组
     * @param length
     * @param sKey   密钥
     * @return 解密后的字节数组
     */
    public static byte[] decrypt(byte[] sSrc, int length, String sKey, String iv, String method) throws Exception {
        if (AesUtils.isNotEmpty(method) && !method.contains("AES"))
            throw new Exception("未知的算法！");
        // 判断Key是否正确
        if (AesUtils.isEmpty(sKey))
            return null;
        // 判断Key是否为16位
        if (sKey.length() != 16 ) {
            throw new Exception("Key长度不是16位！");
        }
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        SecretKeySpec keySpec = new SecretKeySpec( sKey.getBytes(StandardCharsets.UTF_8), "AES");
        byte[] ivByte;
        if (iv.startsWith("0x"))
            ivByte = AesUtils.hexStringToByteArray(iv.substring(2));
        else ivByte = iv.getBytes();
        if (ivByte.length != 16)
            ivByte = new byte[16];
        //如果m3u8有IV标签，那么IvParameterSpec构造函数就把IV标签后的内容转成字节数组传进去
        AlgorithmParameterSpec paramSpec = new IvParameterSpec(ivByte);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
        return cipher.doFinal(sSrc, 0, length);
    }
}
