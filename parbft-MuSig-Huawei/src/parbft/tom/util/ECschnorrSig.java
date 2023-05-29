package parbft.tom.util;

import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Locale;

public class ECschnorrSig {

	private static final String RANDOM_NUMBER_ALGORITHM = "SHA1PRNG";
	private static final String RANDOM_NUMBER_ALGORITHM_PROVIDER = "SUN";
	private static final String SECP256K1 = "secp256k1";
	// Generate a parameter spec representing the passed in named curve, namely SECP256K1.
	private static ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec(SECP256K1);
	// Define the max private key.
	public static final BigInteger LOWERBOUND = BigInteger.ZERO;
	public static final BigInteger UPPERBOUND = spec.getN().subtract(BigInteger.ONE);
	private static final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
	
	// Define curve order.
	public static final BigInteger n = spec.getN();
	public static BigInteger[] multiSigSet = new BigInteger[2];

	public static ECCurve getCurve(){
		return spec.getCurve();
	}
	/*
	 * Define a private key by generating a random value in [1, n-1].
	 */
	public static BigInteger genPrivateKey() {
		SecureRandom secureRandom;
		try {
			secureRandom = SecureRandom.getInstance(RANDOM_NUMBER_ALGORITHM, RANDOM_NUMBER_ALGORITHM_PROVIDER);
		} catch (Exception e) {
			secureRandom = new SecureRandom();
		}
		// Generate the key, skipping as many as desired.
		byte[] privateKeyAttempt = new byte[32];
		// Generates a user-specified number of random bytes.
		secureRandom.nextBytes(privateKeyAttempt);
		// Translates the sign-magnitude representation of a BigInteger into a BigInteger.
		BigInteger privateKeyCheck = new BigInteger(1, privateKeyAttempt);
		// Guarantee private key in interval [1,n-1]
		while (privateKeyCheck.compareTo(LOWERBOUND) == 0 || privateKeyCheck.compareTo(UPPERBOUND) == 1) {
			secureRandom.nextBytes(privateKeyAttempt);
			privateKeyCheck = new BigInteger(1, privateKeyAttempt);
		}
		return privateKeyCheck;
	}

	/*
	 * Define a public key by a given private key: sk = G*pk
	 */
	public static ECPoint genPublicKey(BigInteger privateKey) {
		try {
			return spec.getG().multiply(privateKey);
		} catch (Exception e) {
			return null;
		}
	}

	/*
	 *  Generate k, the same process to generate private key.
	 */
	public static BigInteger genRandomK() {
		SecureRandom secureRandom;
		try {
			secureRandom = SecureRandom.getInstance(RANDOM_NUMBER_ALGORITHM, RANDOM_NUMBER_ALGORITHM_PROVIDER);
		} catch (Exception e) {
			secureRandom = new SecureRandom();
		}
		byte[] kAttempt = new byte[32];
		secureRandom.nextBytes(kAttempt);
		BigInteger kCheck = new BigInteger(1, kAttempt);
		// Guarantee k in interval [1, n-1]
		while (kCheck.compareTo(LOWERBOUND) == 0 || kCheck.compareTo(UPPERBOUND) == 1) {
			secureRandom.nextBytes(kAttempt);
			kCheck = new BigInteger(1, kAttempt);
		}
		return kCheck;
	}

	/*
	 *  Generate a Q as a commit.
	 */
	public static ECPoint genCommit(BigInteger k) {
		ECPoint Q = spec.getG().multiply(k);
		return Q;
	}

	/*
	 *  Generate a challenge r.
	 */
	public static BigInteger genChal(ECPoint Q, ECPoint pk, byte[] msg) {
		// Creating the MessageDigest object
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		// Passing data to the created MessageDigest Object
		digest.update(Q.getEncoded(false));
		digest.update(pk.getEncoded(false));
		digest.update(msg);
		// Compute the message digest
		byte[] temp_r = digest.digest();
		BigInteger int_temp_r = new BigInteger(1, temp_r);
		BigInteger r = int_temp_r.mod(n);
		return r;
	}

	/* 
	 * Generate a response for the challenge r.
	 */
	public static BigInteger genResp(BigInteger r, ECPoint Q, ECPoint pk, BigInteger k, BigInteger sk, byte[] msg) {
		// Creating the MessageDigest object.
		MessageDigest digest = null;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		// Passing data to the created MessageDigest Object
		digest.update(Q.getEncoded(false));
		digest.update(pk.getEncoded(false));
		digest.update(msg);

		// Compute the message digest
		BigInteger temp_r = new BigInteger(1, digest.digest());
		BigInteger check_r = temp_r.mod(n);

		BigInteger s = BigInteger.ZERO;
		if (check_r.equals(r)) {
			//s = (k - r*sk) mod n
			s = (k.subtract(r.multiply(sk))).mod(spec.getN());
		} else {
			System.out.println("Challenge failed -----> ");
		}
		return s;
	}
	
	/*
	 * Validate the response s.
	 */
	public static boolean checkResp(BigInteger r, BigInteger s, ECPoint Q, ECPoint pk) {
		ECPoint checkQ=	(spec.getG().multiply(s)).add(pk.multiply(r));
		if (checkQ.equals(Q)) {
			System.out.println("Response is valid!!!");
			return true;
		} else {
			System.out.println("Response is not valid!!!");
			return false;
		}
	}

	/*
	 * Generate a multi-signature.
	 */
	public static BigInteger[] sign(BigInteger r, BigInteger s) {
		multiSigSet[0] = r;
		multiSigSet[1] = s;
		
		return multiSigSet;
	}
	
	/*
	 * Verify the multi-signature.
	 */
	public static boolean verify(BigInteger[] multiSigSet, byte[] msg, ECPoint Q, ECPoint pk) {
		BigInteger r = multiSigSet[0];
		BigInteger s = multiSigSet[1];
		s = s.mod(spec.getN());//&&||
		if ((r.compareTo(LOWERBOUND) == 0 || r.compareTo(UPPERBOUND) == -1)
				&& (s.compareTo(LOWERBOUND) == 0 || s.compareTo(UPPERBOUND) == -1)) {
			if (checkResp(r, s, Q, pk)) {
				MessageDigest digest = null;
				try {
					digest = MessageDigest.getInstance("SHA-256");
				} catch (NoSuchAlgorithmException e) {
					e.printStackTrace();
				}
				// Passing data to the created MessageDigest Object
				digest.update(Q.getEncoded(false));//Q.toString().getBytes()
				digest.update(pk.getEncoded(false));//pk.toString().getBytes()
				digest.update(msg);
				// Compute the message digest

				BigInteger temp_r = new BigInteger(1, digest.digest());
				BigInteger check_r = temp_r.mod(spec.getN());
				
				if (check_r.equals(r)) {
					System.out.println("Multi-signature is valid!!!");
					return true;
				} else {
					System.out.println("Multi-signature is not valid!!!");
					return false;
				}
			} else {
				System.out.println("Response is not valid!!!");
				return false;
			}
		} else {
			System.out.println("Multi-signature: out of bound!!!");
			return false;
		}
	}
	
	/*
	 * Convert a byte array to a string.
	 */
	public static String toHexString(byte[] data) {
		if (data == null) {
			return "";
		}
		char[] chars = new char[data.length * 2];
		for (int i = 0; i < data.length; i++) {
			chars[i * 2] = HEX_DIGITS[(data[i] >> 4) & 0xf];
			chars[i * 2 + 1] = HEX_DIGITS[data[i] & 0xf];
		}
		    return new String(chars).toLowerCase(Locale.US);
	}

	public static void main(String[] args) {
		// Test for single ECSchnorr signature.
//		System.out.println("-------------------- Single signer test --------------------");
//		BigInteger sk = ECSchnorrSig.genPrivateKey();
//		ECPoint pk = ECSchnorrSig.genPublicKey(sk);
//		BigInteger k = ECSchnorrSig.genRandomK();		
//		ECPoint Q = ECSchnorrSig.genCommit(k);
//		System.out.println("Generating key pair...");
//		System.out.println("sk:" + sk.toString(16));
//		System.out.println("pk:" + toHexString(pk.getEncoded(false)));
//		System.out.println("\nGenerating random k...");
//		System.out.println("k :" + k.toString());
//		System.out.println("\nGenerating commit Q...");
//		System.out.println("Q :" + toHexString(Q.getEncoded(false)));
//		
//		String msg = "test";
//		BigInteger r = ECSchnorrSig.genChal(Q, pk, msg);
//		System.out.println("\nGenerating challenge r...");
//		System.out.println("r :" + r.toString());		
//		
//		BigInteger s = ECSchnorrSig.genResp(r, Q, pk, k, sk, msg);
//		System.out.println("\nGenerating response s...");
//		System.out.println("s:"+s.toString());
//		
//		System.out.println("\nValidating response s...");
//		ECSchnorrSig.checkResp(r, s, Q, pk);
//		
//		System.out.println("\nGenerating multi-signature <r,s>...");
//		BigInteger[] muSigSet = ECSchnorrSig.sign(r, s);
//		System.out.println("r: " + muSigSet[0].toString());
//		System.out.println("s: " + muSigSet[1].toString());
//		
//		System.out.println("\nVerifying multi-signature <r,s>...");
//		ECSchnorrSig.verify(muSigSet, msg, Q, pk);
		
		//Test for multiple ECSchnorr signature.
		System.out.println("-------------------- Multiple signer test --------------------\n");
		BigInteger sk1 = ECschnorrSig.genPrivateKey();
		ECPoint pk1 = ECschnorrSig.genPublicKey(sk1);
		BigInteger k1 = ECschnorrSig.genRandomK();		
		ECPoint Q1 = ECschnorrSig.genCommit(k1);
		BigInteger sk2 = ECschnorrSig.genPrivateKey();
		ECPoint pk2 = ECschnorrSig.genPublicKey(sk2);
		BigInteger k2 = ECschnorrSig.genRandomK();		
		ECPoint Q2 = ECschnorrSig.genCommit(k2);

		byte[] bypk1 = pk1.getEncoded(false);
		byte[] byQ1 = Q1.getEncoded(false);
		byte[] bypk2 = pk2.getEncoded(false);
		byte[] byQ2 = Q2.getEncoded(false);
		// Aggregate pk
		ECPoint pk = getCurve().decodePoint(bypk1).add(getCurve().decodePoint(bypk2));
		System.out.println("Aggregated pk :" + toHexString(pk.getEncoded(false)));
		// Aggregate Q
		ECPoint Q = getCurve().decodePoint(byQ1).add(getCurve().decodePoint(byQ2));
		System.out.println("Aggregated Q  :" + toHexString(Q.getEncoded(false)));
		String msg = "test";
		// Aggregate r
		BigInteger r = ECschnorrSig.genChal(Q, pk, msg.getBytes());

		byte[] encodedQ = Q.getEncoded(false);
		byte[] encodedpk = pk.getEncoded(false);
		ECPoint ecPoint = getCurve().decodePoint(encodedpk);
		ECPoint ecPointQ = getCurve().decodePoint(encodedQ);
		System.out.println(ecPoint.equals(pk));



		BigInteger s1 = ECschnorrSig.genResp(r, ecPointQ, ecPoint, k1, sk1, msg.getBytes());
		BigInteger s2 = ECschnorrSig.genResp(r, ecPointQ, ecPoint, k2, sk2, msg.getBytes());
		ECschnorrSig.checkResp(r, s1, Q1, pk1);
		ECschnorrSig.checkResp(r, s2, Q2, pk2);
		// Aggregate s
		BigInteger s = s1.add(s2);
		// Aggregate muSig set
		BigInteger[] muSigSet = ECschnorrSig.sign(r, s);
		System.out.println("Aggregated r : " + muSigSet[0].toString());
		System.out.println("Aggregated s : " + muSigSet[1].toString());
		// Verify muSig
		ECschnorrSig.verify(muSigSet, msg.getBytes(), Q, pk);
	}
}
