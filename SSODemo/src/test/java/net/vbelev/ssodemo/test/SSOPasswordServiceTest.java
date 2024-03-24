package net.vbelev.ssodemo.test;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

//import junit.framework.Assert;
import net.vbelev.sso.core.*;
import net.vbelev.utils.Utils;

class SSOPasswordServiceTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@AfterEach
	void tearDown() throws Exception {
	}

	//@Test
	void testPasswords() throws java.security.GeneralSecurityException 
	{
		SSOPasswordService sso = new SSOPasswordService();
		long keyId = sso.generateKeyInfo();
		//String pwd1 = "mypassword";
		String pwd1 = Utils.randomString(1000);
		String pwd2 = "anotherpassword";
		SSOPasswordService.PasswordInfo pi = sso.bakePassword(pwd1);

		System.out.println("encrypted pwd=" + pi.getPassword());		
		Assertions.assertTrue(sso.testPassword(pwd1, pi));
		
		//for (int i = 0; i < 100000; i++)
		//{
		//	String pwd3 = Utils.randomString(1000);
		//	boolean t = sso.testPassword(pwd3,  pi);
		//}
		assertFalse(sso.testPassword(pwd2, pi));
	}

	/**
	 * Test that SSO keys can be correctly saved and loaded from an external storage
	 * @throws java.security.GeneralSecurityException
	 */
	//@Test 
	void testKeyStorage() throws java.security.GeneralSecurityException
	{
		
		SSOPasswordService sso = new SSOPasswordService();
		long keyId = sso.generateKeyInfo();
		String pwd1 = Utils.randomString(1000);
		String pwd2 = "anotherpassword";
		SSOPasswordService.PasswordInfo pi = sso.bakePassword(pwd1);
		
		//SSOPasswordService.PasswordInfo pi2 = new SSOPasswordService.PasswordKeyInfo(pi.getPasswordKeyId(), pi.getG, generationTime, keyEncoded, isPublic)
		SSOPasswordService.PasswordKeyInfo keyInfo = sso.getPasswordKeyInfo(pi.getPasswordKeyId());

		// now create and load a new SSO
		SSOPasswordService sso2 = new SSOPasswordService();
		SSOPasswordService.PasswordKeyInfo keyInfo2 = new SSOPasswordService.PasswordKeyInfo(keyInfo.getKeyId(),  keyInfo.getGenerationTime(),  keyInfo.getKeyString(),  true);

		sso2.setPasswordKeyInfo(keyInfo2);
		boolean res = sso2.testPassword(pwd1,  pi);
		boolean res2 = sso2.testPassword(pwd2,  pi);
		assertTrue(res);
		assertFalse(res2);
	}
	
	//@Test
	void simpleAuthRequest()
	{
		long requesterId = 1;
		long authenticatorId = 2;
		long identityId = 3;
		
		SSOAuthService auth = new SSOAuthService();
		// create a request
		SSOAuthService.AuthResponse resp1 = auth.requestAuth(requesterId, null, null);
		String requestToken = resp1.requestToken;
		SSOAuthService.AuthRequestResponse resp2 = auth.getRequestInfo(requesterId, requestToken);
		// check the request status - should still be new
		assertTrue(resp2.status.equals("NEW"));
		// somebody else is checking the status and gets notfound
		SSOAuthService.AuthRequestResponse resp2_1 = auth.getRequestInfo(authenticatorId, requestToken);
		assertTrue(resp2_1.status.equals("NOTFOUND"));
		// the next step is for somebody to process the request and link it to an authentication
		SSOAuthService.AuthRequestResponse resp3 = auth.updateRequestStatus(authenticatorId, requestToken, identityId);
		SSOAuthService.AuthRequestResponse resp4 = auth.getRequestInfo(requesterId, requestToken);
		
		//auth.getAuthInfo(authToken)
		System.out.println("resp3: " + resp3.status + ", resp4: " + resp4.status);
		//auth.
	}
	
	String stringAuthResponse(SSOAuthService.AuthResponse resp)
	{
		return "request token=" + resp.requestToken + ", identityid=" + resp.authenticatedIID + ", auth token=" + resp.authToken + ", status=" + resp.status;
	}

	String stringRequestResponse(SSOAuthService.AuthRequestResponse resp)
	{
		return "request token=" + resp.requestToken + ", identityId=" + resp.requestedIID + ", status=" + resp.status;
	}

	/**
	 * The authenticator app
	 */
	long masterAppId = 1;
	/**
	 * The primary client app (the one that wants to authenticate a user) 
	 */
	long clientAppId = 2;
	/**
	 * The alternative client app (to steal a token or to receive an auth transfer)
	 */
	long otherAppId = 3;
	/**
	 * The primary authenticated user
	 */
	long identityId = 8;

	/**
	 * The basic auth transfer scenario: masterAppId issues a transfer token, and clientAppIdd redeems it successfully.
	 */
	//@Test
	void redeemAuthStraight()
	{
		SSOAuthService auth = new SSOAuthService();
		// generate the transfer request token
		SSOAuthService.AuthResponse resp1 = auth.transferAuth(masterAppId, identityId, clientAppId);
		Assertions.assertEquals("NEW", resp1.status); // there is no "redeeming" status in AuthInfo, so it simply returns "new".
		String requestToken = resp1.requestToken;
		// The client checks the request status - the do _not_ have access to the request info		
		SSOAuthService.AuthRequestResponse resp2 = auth.getRequestInfo(clientAppId, requestToken);
		Assertions.assertEquals("NOTFOUND", resp2.status);
		
		// The client redeems the token
		SSOAuthService.AuthResponse resp3 = auth.redeemRequestToken(clientAppId, requestToken);
		Assertions.assertEquals("ACTIVE", resp3.status);
		Assertions.assertNotNull(resp3.authToken);
		
		// with the received token, get the user id
		String authToken = resp3.authToken;
		SSOAuthService.AuthResponse resp4 = auth.testAuthentication(clientAppId, authToken);
		Assertions.assertEquals(identityId,  resp4.authenticatedIID);
	}
	
	//@Test
	void redeemAuth()
	{
		
		SSOAuthService auth = new SSOAuthService();
		// generate the transfer request token
		SSOAuthService.AuthResponse resp1 = auth.transferAuth(masterAppId, identityId, clientAppId);
		String requestToken = resp1.requestToken;
		System.out.println("resp1 " + stringAuthResponse(resp1));
		// check the request status
		// - masterid
		SSOAuthService.AuthRequestResponse resp2 = auth.getRequestInfo(masterAppId, requestToken);
		System.out.println("resp2 " + stringRequestResponse(resp2));
		// - clientid
		SSOAuthService.AuthRequestResponse resp3 = auth.getRequestInfo(clientAppId, requestToken);
		System.out.println("resp3 " + stringRequestResponse(resp3));
		// - otherid
		SSOAuthService.AuthRequestResponse resp4 = auth.getRequestInfo(otherAppId, requestToken);
		System.out.println("resp4 " + stringRequestResponse(resp4));
		// redeem the token by master
		SSOAuthService.AuthResponse resp5 = auth.redeemRequestToken(masterAppId, requestToken);
		System.out.println("resp5 " + stringAuthResponse(resp5));
		// redeem the token by client
		SSOAuthService.AuthResponse resp6 = auth.redeemRequestToken(clientAppId, requestToken);
		System.out.println("resp6 " + stringAuthResponse(resp6));
		String authToken = resp6.authToken;
		// check the auth status
		SSOAuthService.AuthResponse resp7 = auth.testAuthentication(clientAppId, authToken);
		System.out.println("resp7 " + stringAuthResponse(resp7));
		// check the auth status from other id
		SSOAuthService.AuthResponse resp8 = auth.testAuthentication(otherAppId, authToken);
		System.out.println("resp8 " + stringAuthResponse(resp7));
		// redeem again
		SSOAuthService.AuthResponse resp9 = auth.redeemRequestToken(clientAppId, requestToken);
		System.out.println("resp9 " + stringAuthResponse(resp9));
		// check the auth status again
		SSOAuthService.AuthResponse resp10 = auth.testAuthentication(clientAppId, authToken);
		System.out.println("resp10 " + stringAuthResponse(resp10));
	
	}
	
	//@Test
	void failoverTest() throws IOException
	{
        String homeFolder = System.getenv("APPDATA");
        if (Utils.isBlank(homeFolder))
            homeFolder = System.getProperty("user.home");
        String failoverFolder = Path.of(homeFolder, "SSOFailoverTest").toAbsolutePath().toString();
        SSOFailover failover = new SSOFailover(failoverFolder, "auth", null, 5);
        failover.reset();
        SSOAuthService auth1 = new SSOAuthService(failoverFolder);	
        
        // create a request
        SSOAuthService.AuthResponse resp1 = auth1.requestAuth(clientAppId, null, null);
        String requestToken = resp1.requestToken;
        // the next step is for somebody to process the request and link it to an authentication
        SSOAuthService.AuthRequestResponse resp3 = auth1.updateRequestStatus(masterAppId, requestToken, identityId);
        // abandon auth1 and start auth2 
        SSOAuthService auth2 = new SSOAuthService(failoverFolder);
        SSOAuthService.AuthRequestResponse resp4 = auth2.getRequestInfo(clientAppId, requestToken);
        
        System.out.println("resp3: " + resp3.status + ", resp4: " + resp4.status);
	}
	
	@Test
	void propertyTest() throws IOException
	{
        String homeFolder = System.getenv("APPDATA");
        if (Utils.isBlank(homeFolder))
            homeFolder = System.getProperty("user.home");
        String propertiesPath = Path.of(homeFolder, "SSOFailoverTest", "failover.properties").toAbsolutePath().toString();
        Properties appProps = new Properties();
        appProps.load(new FileInputStream(propertiesPath));
	}
}
