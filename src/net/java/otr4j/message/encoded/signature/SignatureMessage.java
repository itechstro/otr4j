package net.java.otr4j.message.encoded.signature;

import java.io.*;
import java.security.*;
import javax.crypto.*;

import net.java.otr4j.message.*;
import net.java.otr4j.message.encoded.*;

public final class SignatureMessage extends SignatureMessageBase {

	public SignatureMessage(int protocolVersion, byte[] xEncryptedMAC, byte[] xEncrypted)
			throws NoSuchAlgorithmException, InvalidKeyException,
			NoSuchPaddingException, InvalidAlgorithmParameterException,
			IllegalBlockSizeException, BadPaddingException, SignatureException {

		this.setMessageType(MessageType.SIGNATURE);
		this.setProtocolVersion(protocolVersion);
		this.setXEncryptedMAC(xEncryptedMAC);
		this.setXEncrypted(xEncrypted);
	}

	public SignatureMessage() {

	}

	public void writeObject(java.io.ByteArrayOutputStream stream)
			throws IOException {

		SerializationUtils.writeShort(stream, this.getProtocolVersion());
		SerializationUtils.writeByte(stream, this.getMessageType());
		SerializationUtils.writeData(stream, this.getXEncrypted());
		SerializationUtils.writeMac(stream, this.getXEncryptedMAC());
	}

	public void readObject(java.io.ByteArrayInputStream stream) throws IOException {

		this.setProtocolVersion(DeserializationUtils.readShort(stream));
		this.setMessageType(DeserializationUtils.readByte(stream));
		this.setXEncrypted(DeserializationUtils.readData(stream));
		this.setXEncryptedMAC(DeserializationUtils.readMac(stream));
	}
}
