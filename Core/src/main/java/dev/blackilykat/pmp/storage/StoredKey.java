/*
 * Copyright (C) 2026 Blackilykat and contributors
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

package dev.blackilykat.pmp.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Key;
import java.util.Base64;

public class StoredKey extends Stored<String> {
	private static final Logger LOGGER = LogManager.getLogger(StoredKey.class);

	public StoredKey(Storage storage) {
		super(String.class, storage, null);
	}

	public Key getDecoded() {
		String got = get();
		if(got == null) {
			return null;
		}

		byte[] data = Base64.getDecoder().decode(got);
		try(ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(data))) {
			Object o = is.readObject();
			if(!(o instanceof Key key)) {
				LOGGER.error("ClientStorage$Sensitive: Key is not a key but a {}, this should be unreachable",
						o.getClass().getName());
				return null;
			}
			return key;
		} catch(IOException | ClassNotFoundException e) {
			LOGGER.error("ClientStorage$Sensitive: this should be unreachable", e);
			return null;
		}
	}

	public void setDecoded(Key key) {
		if(get() != null) {
			LOGGER.warn("Overriding server public key");
		}
		try(ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			ObjectOutputStream os = new ObjectOutputStream(byteStream)) {

			os.writeObject(key);
			byte[] data = byteStream.toByteArray();
			set(Base64.getEncoder().encodeToString(data));
		} catch(IOException e) {
			LOGGER.error("Storage#getServerPublicKey: this should be unreachable", e);
		}
	}
}
