package com.schoolmanager.backend.crypto;

import com.schoolmanager.backend.config.AppProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class AesCryptoService {
	private static final String TRANSFORM = "AES/GCM/NoPadding";
	private static final int IV_LEN = 12;
	private static final int TAG_LEN_BITS = 128;

	private final SecretKey key;
	private final SecureRandom random = new SecureRandom();

	public AesCryptoService(AppProperties props) {
		byte[] keyBytes = Base64.getDecoder().decode(props.getCrypto().getAesKeyBase64());
		this.key = new SecretKeySpec(keyBytes, "AES");
	}

	public String encryptToBase64(String plainText) {
		if (plainText == null || plainText.isBlank()) {
			return null;
		}
		try {
			byte[] iv = new byte[IV_LEN];
			random.nextBytes(iv);

			Cipher cipher = Cipher.getInstance(TRANSFORM);
			cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));
			byte[] ct = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

			byte[] out = new byte[iv.length + ct.length];
			System.arraycopy(iv, 0, out, 0, iv.length);
			System.arraycopy(ct, 0, out, iv.length, ct.length);
			return Base64.getEncoder().encodeToString(out);
		} catch (Exception e) {
			throw new IllegalStateException("AES_ENCRYPT_FAILED", e);
		}
	}

	public String decryptFromBase64(String base64) {
		if (base64 == null || base64.isBlank()) {
			return null;
		}
		try {
			byte[] in = Base64.getDecoder().decode(base64);
			if (in.length <= IV_LEN) {
				return null;
			}
			byte[] iv = new byte[IV_LEN];
			byte[] ct = new byte[in.length - IV_LEN];
			System.arraycopy(in, 0, iv, 0, IV_LEN);
			System.arraycopy(in, IV_LEN, ct, 0, ct.length);

			Cipher cipher = Cipher.getInstance(TRANSFORM);
			cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LEN_BITS, iv));
			byte[] pt = cipher.doFinal(ct);
			return new String(pt, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("AES_DECRYPT_FAILED", e);
		}
	}
}
