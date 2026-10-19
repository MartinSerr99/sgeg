package com.sgeg.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contraseñas (RNF09: nunca se guardan en texto plano).
 * Usa PBKDF2-HMAC-SHA256 de la librería estándar de Java, con sal aleatoria.
 *
 * Formato almacenado:  pbkdf2_sha256$iteraciones$sal(Base64)$hash(Base64)
 * Como las iteraciones viajan dentro del hash, se pueden aumentar en el futuro
 * sin invalidar las contraseñas ya guardadas.
 */
public final class SeguridadUtil {

    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final String PREFIJO = "pbkdf2_sha256";
    private static final int ITERACIONES = 600_000;
    private static final int BYTES_SAL = 16;
    private static final int BYTES_HASH = 32;
    private static final SecureRandom ALEATORIO = new SecureRandom();

    private SeguridadUtil() {
    }

    public static String generarHash(String contrasenia) {
        byte[] sal = new byte[BYTES_SAL];
        ALEATORIO.nextBytes(sal);
        byte[] hash = derivar(contrasenia, sal, ITERACIONES, BYTES_HASH);
        return String.join("$", PREFIJO, String.valueOf(ITERACIONES),
                Base64.getEncoder().encodeToString(sal),
                Base64.getEncoder().encodeToString(hash));
    }

    public static boolean verificar(String contrasenia, String hashAlmacenado) {
        if (contrasenia == null || hashAlmacenado == null) {
            return false;
        }
        String[] partes = hashAlmacenado.split("\\$");
        if (partes.length != 4 || !PREFIJO.equals(partes[0])) {
            return false;
        }
        try {
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = derivar(contrasenia, sal, iteraciones, esperado.length);
            // Comparación en tiempo constante: evita filtrar información por tiempos de respuesta.
            return MessageDigest.isEqual(esperado, calculado);
        } catch (IllegalArgumentException formatoInvalido) {
            return false;
        }
    }

    private static byte[] derivar(String contrasenia, byte[] sal, int iteraciones, int bytesHash) {
        PBEKeySpec especificacion = new PBEKeySpec(contrasenia.toCharArray(), sal, iteraciones, bytesHash * 8);
        try {
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(especificacion).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña", e);
        } finally {
            especificacion.clearPassword();
        }
    }
}
