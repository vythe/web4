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
		
		SSOAuthProvider auth = new SSOAuthProvider();
		SSOAuthRequestProvider authReq = new SSOAuthRequestProvider(auth);
		// create a request
		SSOAuthProvider.AuthResponse resp1 = authReq.requestAuth(requesterId, null, null);
		String requestToken = resp1.requestToken;
		SSOAuthRequestProvider.AuthRequestResponse resp2 = authReq.getRequestInfo(requesterId, requestToken);
		// check the request status - should still be new
		assertTrue(resp2.status.equals("NEW"));
		// somebody else is checking the status and gets notfound
		SSOAuthRequestProvider.AuthRequestResponse resp2_1 = authReq.getRequestInfo(authenticatorId, requestToken);
		assertTrue(resp2_1.status.equals("NOTFOUND"));
		// the next step is for somebody to process the request and link it to an authentication
		SSOAuthRequestProvider.AuthRequestResponse resp3 = authReq.updateRequestStatus(authenticatorId, requestToken, identityId, "test user", "testuser");
		SSOAuthRequestProvider.AuthRequestResponse resp4 = authReq.getRequestInfo(requesterId, requestToken);
		
		//auth.getAuthInfo(authToken)
		System.out.println("resp3: " + resp3.status + ", resp4: " + resp4.status);
		//auth.
	}
	
	String stringAuthResponse(SSOAuthProvider.AuthResponse resp)
	{
		return "request token=" + resp.requestToken + ", identityid=" + resp.authenticatedIID + ", auth token=" + resp.authToken + ", status=" + resp.status;
	}

	String stringRequestResponse(SSOAuthRequestProvider.AuthRequestResponse resp)
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
		SSOAuthProvider auth = new SSOAuthProvider();
		SSOAuthRequestProvider authReq = new SSOAuthRequestProvider(auth);
		// generate the transfer request token
		SSOAuthProvider.AuthResponse resp1 = authReq.transferAuth(masterAppId, identityId, clientAppId);
		Assertions.assertEquals("NEW", resp1.status); // there is no "redeeming" status in AuthInfo, so it simply returns "new".
		String requestToken = resp1.requestToken;
		// The client checks the request status - the do _not_ have access to the request info		
		SSOAuthRequestProvider.AuthRequestResponse resp2 = authReq.getRequestInfo(clientAppId, requestToken);
		Assertions.assertEquals("NOTFOUND", resp2.status);
		
		// The client redeems the token
		SSOAuthProvider.AuthResponse resp3 = authReq.redeemRequestToken(clientAppId, requestToken);
		Assertions.assertEquals("ACTIVE", resp3.status);
		Assertions.assertNotNull(resp3.authToken);
		
		// with the received token, get the user id
		String authToken = resp3.authToken;
		SSOAuthProvider.AuthResponse resp4 = auth.testAuthentication(clientAppId, authToken);
		Assertions.assertEquals(identityId,  resp4.authenticatedIID);
	}
	
	@Test
	void redeemAuth()
	{
		
		SSOAuthProvider auth = new SSOAuthProvider();
		SSOAuthRequestProvider authReq = new SSOAuthRequestProvider(auth);
		
		// generate the transfer request token
		SSOAuthProvider.AuthResponse resp1 = authReq.transferAuth(masterAppId, identityId, clientAppId);
		String requestToken = resp1.requestToken;
		System.out.println("resp1 " + stringAuthResponse(resp1));
		// check the request status
		// - masterid
		SSOAuthRequestProvider.AuthRequestResponse resp2 = authReq.getRequestInfo(masterAppId, requestToken);
		System.out.println("resp2 " + stringRequestResponse(resp2));
		// - clientid
		SSOAuthRequestProvider.AuthRequestResponse resp3 = authReq.getRequestInfo(clientAppId, requestToken);
		System.out.println("resp3 " + stringRequestResponse(resp3));
		// - otherid
		SSOAuthRequestProvider.AuthRequestResponse resp4 = authReq.getRequestInfo(otherAppId, requestToken);
		System.out.println("resp4 " + stringRequestResponse(resp4));
		// redeem the token by master
		SSOAuthProvider.AuthResponse resp5 = authReq.redeemRequestToken(masterAppId, requestToken);
		System.out.println("resp5 " + stringAuthResponse(resp5));
		// redeem the token by client
		SSOAuthProvider.AuthResponse resp6 = authReq.redeemRequestToken(clientAppId, requestToken);
		System.out.println("resp6 " + stringAuthResponse(resp6));
		String authToken = resp6.authToken;
		// check the auth status
		SSOAuthProvider.AuthResponse resp7 = auth.testAuthentication(clientAppId, authToken);
		System.out.println("resp7 " + stringAuthResponse(resp7));
		// check the auth status from other id
		SSOAuthProvider.AuthResponse resp8 = auth.testAuthentication(otherAppId, authToken);
		System.out.println("resp8 " + stringAuthResponse(resp7));
		// redeem again
		SSOAuthProvider.AuthResponse resp9 = authReq.redeemRequestToken(clientAppId, requestToken);
		System.out.println("resp9 " + stringAuthResponse(resp9));
		// check the auth status again
		SSOAuthProvider.AuthResponse resp10 = auth.testAuthentication(clientAppId, authToken);
		System.out.println("resp10 " + stringAuthResponse(resp10));
	
	}
	
	@Test
	void failoverTest() throws IOException
	{
        String homeFolder = System.getenv("APPDATA");
        if (Utils.isBlank(homeFolder))
            homeFolder = System.getProperty("user.home");
        String failoverFolder = Path.of(homeFolder, "SSOFailoverTest").toAbsolutePath().toString();
        SSOFailover failover = new SSOFailover(failoverFolder, "auth", null, 5);
        failover.reset();
        SSOAuthProvider auth1 = new SSOAuthProvider(failoverFolder);	
        SSOAuthRequestProvider authReq1 = new SSOAuthRequestProvider(auth1, failoverFolder);
        
        // create a request
        SSOAuthProvider.AuthResponse resp1 = authReq1.requestAuth(clientAppId, null, null);
        String requestToken = resp1.requestToken;
        // the next step is for somebody to process the request and link it to an authentication
        SSOAuthRequestProvider.AuthRequestResponse resp3 = authReq1.updateRequestStatus(masterAppId, requestToken, identityId, "test user", "testuser");
        // abandon auth1 and start auth2 
        SSOAuthProvider auth2 = new SSOAuthProvider(failoverFolder);
        SSOAuthRequestProvider authReq2 = new SSOAuthRequestProvider(auth2, failoverFolder);
        SSOAuthRequestProvider.AuthRequestResponse resp4 = authReq2.getRequestInfo(clientAppId, requestToken);
        
        System.out.println("resp3: " + resp3.status + ", resp4: " + resp4.status);
	}
	
	//@Test
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
