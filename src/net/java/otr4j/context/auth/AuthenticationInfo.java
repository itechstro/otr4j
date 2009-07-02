package net.java.otr4j.context.auth;

import java.io.*;
import java.math.*;
import java.nio.*;
import java.security.*;
import javax.crypto.*;
import javax.crypto.interfaces.*;

import org.apache.log4j.*;

import net.java.otr4j.*;
import net.java.otr4j.crypto.*;
import net.java.otr4j.message.encoded.*;

public class AuthenticationInfo {

	private static Logger logger = Logger.getLogger(AuthenticationInfo.class);

	public AuthenticationInfo() {
		this.reset();
	}

	private AuthenticationState authenticationState;
	private byte[] r;

	private DHPublicKey remoteDHPublicKey;
	private int remoteDHPPublicKeyID;
	private byte[] remoteDHPublicKeyEncrypted;
	private byte[] remoteDHPublicKeyHash;

	private KeyPair localDHKeyPair;
	private int localDHPrivateKeyID;
	private byte[] localDHPublicKeyBytes;
	private byte[] localDHPublicKeyHash;
	private byte[] localDHPublicKeyEncrypted;

	private BigInteger s;
	private byte[] c;
	private byte[] m1;
	private byte[] m2;
	private byte[] cp;
	private byte[] m1p;
	private byte[] m2p;

	private byte[] localXEncrypted;
	private byte[] localXEncryptedMac;

	private KeyPair localLongTermKeyPair;

	public void reset() {
		logger.info("Resetting authentication state.");
		authenticationState = AuthenticationState.NONE;
		r = null;

		remoteDHPublicKey = null;
		remoteDHPPublicKeyID = -1;
		remoteDHPublicKeyEncrypted = null;
		remoteDHPublicKeyHash = null;

		localDHKeyPair = null;
		localDHPrivateKeyID = 1;
		localDHPublicKeyBytes = null;
		localDHPublicKeyHash = null;
		localDHPublicKeyEncrypted = null;

		s = null;
		c = m1 = m2 = cp = m1p = m2p = null;

		localXEncrypted = null;
		localXEncryptedMac = null;

		localLongTermKeyPair = null;
	}

	public void setAuthenticationState(AuthenticationState authenticationState) {
		this.authenticationState = authenticationState;
	}

	public AuthenticationState getAuthenticationState() {
		return authenticationState;
	}

	public byte[] getR() {
		if (r == null) {
			logger.info("Picking random key r.");
			r = Utils.getRandomBytes(CryptoConstants.AES_KEY_BYTE_LENGTH);
		}
		return r;
	}

	public void setRemoteDHPublicKey(DHPublicKey remoteDHPublicKey) {
		this.remoteDHPublicKey = remoteDHPublicKey;
	}

	public DHPublicKey getRemoteDHPublicKey() {
		return remoteDHPublicKey;
	}

	public void setRemoteDHPublicKeyEncrypted(byte[] remoteDHPublicKeyEncrypted) {
		logger.info("Storing encrypted remote public key.");
		this.remoteDHPublicKeyEncrypted = remoteDHPublicKeyEncrypted;
	}

	public byte[] getRemoteDHPublicKeyEncrypted() {
		return remoteDHPublicKeyEncrypted;
	}

	public void setRemoteDHPublicKeyHash(byte[] remoteDHPublicKeyHash) {
		logger.info("Storing encrypted remote public key hash.");
		this.remoteDHPublicKeyHash = remoteDHPublicKeyHash;
	}

	public byte[] getRemoteDHPublicKeyHash() {
		return remoteDHPublicKeyHash;
	}

	public KeyPair getLocalDHKeyPair() throws NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, NoSuchProviderException {
		if (localDHKeyPair == null) {
			localDHKeyPair = CryptoUtils.generateDHKeyPair();
			logger.info("Generated local D-H key pair.");
		}
		return localDHKeyPair;
	}

	public int getLocalDHKeyPairID() {
		return localDHPrivateKeyID;
	}

	public byte[] getLocalDHPublicKeyHash() throws NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, NoSuchProviderException {
		if (localDHPublicKeyHash == null) {
			localDHPublicKeyHash = CryptoUtils
					.sha256Hash(getLocalDHPublicKeyBytes());
			logger.info("Hashed local D-H public key.");
		}
		return localDHPublicKeyHash;
	}

	public byte[] getLocalDHPublicKeyEncrypted() throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, IllegalBlockSizeException,
			BadPaddingException, NoSuchProviderException {
		if (localDHPublicKeyEncrypted == null) {
			localDHPublicKeyEncrypted = CryptoUtils.aesEncrypt(getR(), null,
					getLocalDHPublicKeyBytes());
			logger.info("Encrypted our D-H public key.");
		}
		return localDHPublicKeyEncrypted;
	}

	public BigInteger getS() throws InvalidKeyException,
			NoSuchAlgorithmException, InvalidAlgorithmParameterException,
			NoSuchProviderException {
		if (s == null) {
			s = CryptoUtils.generateSecret(this.getLocalDHKeyPair()
					.getPrivate(), this.getRemoteDHPublicKey());
			logger.info("Generated shared secret.");
		}
		return s;
	}

	public byte[] getC() throws NoSuchAlgorithmException, IOException {
		if (c != null)
			return c;
		
		byte[] h2 = h2(CryptoConstants.C_START, s);
		ByteBuffer buff = ByteBuffer.wrap(h2);
		this.c = new byte[CryptoConstants.AES_KEY_BYTE_LENGTH];
		buff.get(this.c);
		logger.info("Computed c.");
		return c;

	}

	public byte[] getM1() throws NoSuchAlgorithmException, IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchProviderException {
		if (m1 != null)
			return m1;
		
		byte[] h2 = h2(CryptoConstants.M1_START, this.getS());
		ByteBuffer buff = ByteBuffer.wrap(h2);
		byte[] m1 = new byte[CryptoConstants.SHA256_HMAC_KEY_BYTE_LENGTH];
		buff.get(m1);
		logger.info("Computed m1.");
		this.m1 = m1;
		return m1;
	}

	public byte[] getM2() throws NoSuchAlgorithmException, IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchProviderException {
		if (m2 != null)
			return m2;

		byte[] h2 = h2(CryptoConstants.M2_START, this.getS());
		ByteBuffer buff = ByteBuffer.wrap(h2);
		byte[] m2 = new byte[CryptoConstants.SHA256_HMAC_KEY_BYTE_LENGTH];
		buff.get(m2);
		logger.info("Computed m2.");
		this.m2 = m2;
		return m2;
	}

	public byte[] getCp() throws NoSuchAlgorithmException, IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchProviderException {
		if (cp != null)
			return cp;

		byte[] h2 = h2(CryptoConstants.C_START, this.getS());
		ByteBuffer buff = ByteBuffer.wrap(h2);
		byte[] cp = new byte[CryptoConstants.AES_KEY_BYTE_LENGTH];
		buff.position(CryptoConstants.AES_KEY_BYTE_LENGTH);
		buff.get(cp);
		logger.info("Computed c'.");
		this.cp = cp;
		return cp;
	}

	public byte[] getM1p() throws NoSuchAlgorithmException, IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchProviderException {
		if (m1p != null)
			return m1p;

		byte[] h2 = h2(CryptoConstants.M1p_START, this.getS());
		ByteBuffer buff = ByteBuffer.wrap(h2);
		byte[] m1p = new byte[CryptoConstants.SHA256_HMAC_KEY_BYTE_LENGTH];
		buff.get(m1p);
		this.m1p = m1p;
		logger.info("Computed m1'.");
		return m1p;
	}

	public byte[] getM2p() throws NoSuchAlgorithmException, IOException,
			InvalidKeyException, InvalidAlgorithmParameterException,
			NoSuchProviderException {
		if (m2p != null)
			return m2p;

		byte[] h2 = h2(CryptoConstants.M2p_START, this.getS());
		ByteBuffer buff = ByteBuffer.wrap(h2);
		byte[] m2p = new byte[CryptoConstants.SHA256_HMAC_KEY_BYTE_LENGTH];
		buff.get(m2p);
		this.m2p = m2p;
		logger.info("Computed m2'.");
		return m2p;
	}

	public void setLocalXEncrypted(byte[] localXEncrypted) {
		logger.info("Set local X");
		this.localXEncrypted = localXEncrypted;
	}

	public byte[] getLocalXEncrypted() {
		return localXEncrypted;
	}

	public void setLocalXEncryptedMac(byte[] localXEncryptedMac) {
		this.localXEncryptedMac = localXEncryptedMac;
		logger.info("Set local encrypted X hash.");
	}

	public byte[] getLocalXEncryptedMac() {
		return localXEncryptedMac;
	}

	public void setLocalLongTermKeyPair(KeyPair localLongTermKeyPair) {
		this.localLongTermKeyPair = localLongTermKeyPair;
	}

	public KeyPair getLocalLongTermKeyPair() {
		return localLongTermKeyPair;
	}

	public void setRemoteDHPPublicKeyID(int remoteDHPPublicKeyID) {
		this.remoteDHPPublicKeyID = remoteDHPPublicKeyID;
	}

	public int getRemoteDHPPublicKeyID() {
		return remoteDHPPublicKeyID;
	}

	private static byte[] h2(byte b, BigInteger s)
			throws NoSuchAlgorithmException, IOException {

		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		SerializationUtils.writeMpi(bos, s);
		byte[] secbytes = bos.toByteArray();
		bos.close();

		int len = secbytes.length + 1;
		ByteBuffer buff = ByteBuffer.allocate(len);
		buff.put(b);
		buff.put(secbytes);
		return CryptoUtils.sha256Hash(buff.array());
	}

	public byte[] getLocalDHPublicKeyBytes() throws NoSuchAlgorithmException,
			InvalidAlgorithmParameterException, NoSuchProviderException {
		if (localDHPublicKeyBytes == null)

			localDHPublicKeyBytes = ((DHPublicKey) getLocalDHKeyPair()
					.getPublic()).getY().toByteArray();
		return localDHPublicKeyBytes;
	}
}
