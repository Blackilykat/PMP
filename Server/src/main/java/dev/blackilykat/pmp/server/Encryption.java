/*
 * Copyright (C) 2025 Blackilykat and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.blackilykat.pmp.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v1CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v1CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;

public class Encryption {
	private static final Logger LOGGER = LogManager.getLogger(Encryption.class);
	private static SSLContext sslContext = null;

	public static SSLContext getSslContext() {
		return sslContext;
	}

	public static void init() {
		LOGGER.info("Initializing SSL...");
		try {
			Security.addProvider(new BouncyCastleProvider());
			char[] keyPassword = "key".toCharArray();
			File keyStoreFile = new File("keystore.jks");
			KeyStore keyStore = KeyStore.getInstance("BCFKS", "BC");
			if(!keyStoreFile.exists()) {
				// https://github.com/rodbate/bouncycastle-examples/blob/master/src/main/java/bcfipsin100/tls/Simple.java
				KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
				keyPairGenerator.initialize(2048);
				KeyPair keyPair = keyPairGenerator.generateKeyPair();

				X509v1CertificateBuilder builder = new JcaX509v1CertificateBuilder(new X500Name("CN=PMP Server"),
						BigInteger.valueOf(System.currentTimeMillis()), new Date(System.currentTimeMillis() - 5000L),
						// expires in 2000 years (basically never)
						new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 365 * 2000),
						new X500Name("CN=PMP Server"), keyPair.getPublic());
				JcaContentSignerBuilder signerBuilder = new JcaContentSignerBuilder("SHA384withRSA").setProvider("BC");
				X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC")
						.getCertificate(builder.build(signerBuilder.build(keyPair.getPrivate())));

				keyStore.load(null, null);
				keyStore.setKeyEntry("Key", keyPair.getPrivate(), keyPassword, new X509Certificate[]{certificate});
				keyStore.store(new FileOutputStream(keyStoreFile), null);
			} else {
				keyStore.load(new FileInputStream(keyStoreFile), null);
			}

			KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509");
			factory.init(keyStore, keyPassword);

			sslContext = SSLContext.getInstance("TLS");
			sslContext.init(factory.getKeyManagers(), null, SecureRandom.getInstance("DEFAULT", "BC"));
		} catch(OperatorCreationException | GeneralSecurityException | IOException e) {
			LOGGER.fatal("Failed to initialize SSL", e);
			System.exit(1);
		}
	}
}

