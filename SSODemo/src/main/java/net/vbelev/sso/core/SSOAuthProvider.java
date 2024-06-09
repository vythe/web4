package net.vbelev.sso.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.vbelev.utils.Utils;

/**
 * Generate, maintain and validate auth tokens, aka private tokens. 
 * Also, generates and exchanges query tokens, aka public tokens.
 * 
 *
 * An application, identified by {@code identityId}, wants to work with an authenticated user.
 * For that, the application gets an authentication token from this service, 
 * which can be used to 1) confirm that the user is still authenticated, 
 * 2) get some public details and the permanent identityId for the authenticated user, 
 * 3) maybe - store and retrieve a small tag with user information just for this application?
 * 
 *   IID = Identity Id.
 *   
 *   Note that this service does not verify requester and approver identities. 
 *   Presumably, the consumer application authenticated them and trusts them.
 */
public class SSOAuthProvider 
{
    private static final long AUTH_EXPIRY_SEC = 100;
    
	
	public static enum AuthStatus
	{
		NOTFOUND,
		NEW,
		NOTREADY,
		ACTIVE,
		EXPIRED,
		CANCELLED
	}
	
	/**
	 * Privately stored authentication info
	 */
	static class AuthInfo
	{
	    /** The auth token, used to authenticate incoming requests; the token is mapped to the IID. */
		public String token;
		
		public long expiryTime;
		/**
		 * Identity Id may be null if the status is not READY.
		 * Originally, IIDs were permanent IDs in some "primary" user database, but now 
		 * 1) IID is an internal value, not visible to consumers;
		 * 2) IID is generated and assigned together with the auth token;
		 * 3) the same IID can have multiple auth tokens, when it is transferred between apps; 
		 * 3) there is no primary user database, but there can be a trusted auth application that registers other auth applications.
		 * 
		 *  ... right?
		 */
		public Long IID;
		/**
		 * Which application authenticated teh user. 
		 * When an auth is requested, the requester may specify which auth application should process it,
		 * or it can leave the auth application not selected, meaning "anybody". This option is not very secure but valid. 
		 */
		public Long authenticatorIID;
        /**
         * The public (non-unique) name of the authenticated identity, used to mask 
         * the non-human-friendly authenticator key on the UI
         */
        public String name;
        /** The key (traditionally, the email) used by the authenticator to recognize the identify behind the IID.*/
        public String authenticatingKey;		
		/**
		 * Do i need the token owner's id?
		 */
		public Long ownerIID;
		
		public AuthStatus status;
		
		
		public AuthInfo()
		{
		}
		
		public AuthInfo(String serialized) throws IOException
		{
		    String[] fields = serialized.split(":");
		    if (fields.length != 8)
		        throw new IOException("Invalid serialized string");
            try
            {
                token = fields[0];
                expiryTime = Utils.parseLong(fields[1]);
                IID = Utils.parseLong(fields[2]);
                authenticatorIID = Utils.parseLong(fields[3]);
                ownerIID = Utils.parseLong(fields[4]);
                status = AuthStatus.valueOf(fields[5]);
                name = Utils.decode64(fields[6]);
                authenticatingKey = Utils.decode64(fields[7]);
            }
		    catch (NumberFormatException x)
		    {
                throw new IOException("Invalid serialized field");
		    }
            catch (IllegalArgumentException x)
            {
                throw new IOException("Invalid serialized field");
            }
		}
		
		public String toSerialized()
		{
		    StringBuilder sb = new StringBuilder(100);
		    sb.append(token);
		    sb.append(":");
            sb.append(expiryTime);
            sb.append(":");
            sb.append(Utils.stringOrEmpty(IID));
            sb.append(":");
            sb.append(Utils.stringOrEmpty(authenticatorIID));
            sb.append(":");
            sb.append(Utils.stringOrEmpty(ownerIID));
            sb.append(":");
            sb.append(status.name());
            sb.append(":");
            sb.append(Utils.encode64(name));
            sb.append(":");
            sb.append(Utils.encode64(authenticatingKey));
            
            return sb.toString();
		}
		
		public boolean isActive()
		{
			return !isExpired() && Utils.InList(status, AuthStatus.NEW, AuthStatus.NOTREADY, AuthStatus.ACTIVE);
		}
		
		public boolean isExpired()
		{
			return expiryTime < new Date().getTime();
		}
				
        public AuthInfo clone()
        {
            AuthInfo res = new AuthInfo();
            res.token = token;
            res.expiryTime = expiryTime;
            res.IID = IID;
            res.authenticatorIID = authenticatorIID;
            res.status = status;
            res.name = name;
            res.authenticatingKey = authenticatingKey;
            return res;
        }
	}
	
	/**
	 * Public response to a new auth request, a status update request or for redeeming a request token
	 */
	public static class AuthResponse
	{
		/**
		 * Request token will be set for new auth requests only
		 */
		public String requestToken;
		/**
		 * The private auth token, it is always set
		 */
		public String authToken;
		/**
		 * The authenticated user's IID
		 */
		public Long authenticatedIID;
		/**
		 * The application that authenticated the user
		 */
		public Long authenticatorIID;
		/**
		 * The expiry time may be the request token's expiry time when not authenticated
		 * or the actual authentication's expiry.
		 */
		public long expiryTime;
		
		public String status;
		
		public String name;
		
		public String authenticatingKey;
		
		public AuthResponse() {}
		
		public AuthResponse(AuthStatus status)
		{
			this.status = status.name();
		}
		
		public AuthResponse(SSOAuthRequestProvider.AuthRequestInfo request)
		{
			this.authToken = null;
			this.authenticatorIID = request.authenticatorIID;
			if (request.isActive())
			{
				this.status = AuthStatus.NEW.name();
				this.expiryTime = request.expiryTime;
				this.requestToken = request.requestToken;
			}
			else			
			{
				this.status = AuthStatus.NOTREADY.name();
				this.expiryTime = 0;
			}
		}
		
		public AuthResponse(AuthInfo auth, SSOAuthRequestProvider.AuthRequestInfo request)
		{
			this.authToken = auth.token;
			this.authenticatorIID = request == null? auth.authenticatorIID : request.authenticatorIID;
			this.name = auth.name;
			this.authenticatingKey = auth.authenticatingKey;
			
			switch (auth.status)
			{
			case NOTFOUND:
				break;
			case NEW:
				if (request != null)
				{
					this.requestToken = request.requestToken;
					this.expiryTime = request.expiryTime;
				}
				break;
			case ACTIVE:
				this.expiryTime = auth.expiryTime;
				this.authenticatedIID = auth.IID;
				break;
			case EXPIRED:
				this.expiryTime = auth.expiryTime;
				break;
			case NOTREADY:
				this.expiryTime = request == null? auth.expiryTime : request.expiryTime;
				break;
			case CANCELLED:
				this.expiryTime = 0;
			}
			this.status = auth.status.name();
		}
		
		public static AuthResponse NOTFOUND()
		{
			return new AuthResponse(AuthStatus.NOTFOUND);
		}
	}
    
	// ###### private fields #######
    private final ReentrantReadWriteLock _authLock = new ReentrantReadWriteLock();
    private final Hashtable<String, AuthInfo> _authTable = new Hashtable<String, AuthInfo>();

    private final SSOFailover _failover; 
	private boolean _failoverEnabled;
    
	private static long _nextIID = 0;
	
	public SSOAuthProvider()
    {
        _failover = null;
        _failoverEnabled = false;
    }
    
    public SSOAuthProvider(String failoverFolder) throws IOException
    {
        //_failover = new SSOFailover(Path.of(homeFolder, "SSOFailoverTest").toAbsolutePath().toString(), "simple", null, 5);
        _failover = new SSOFailover(failoverFolder, "auth", null, 5);
        _failoverEnabled = true;
        Map<String, String> recoveredAuth = _failover.restore();
        for (Map.Entry<String, String> entry : recoveredAuth.entrySet())
        {
            String key = entry.getKey();
            /*if (key.startsWith("req_"))
            {
                AuthRequestInfo req = new AuthRequestInfo(entry.getValue());
                _requestTable.put(req.requestToken, req);                        
            }
            else*/
            if (key.startsWith("auth_"))
            {
                AuthInfo auth = new AuthInfo(entry.getValue());
                _authTable.put(auth.token, auth);
            }
            else               
            {
                throw new IllegalArgumentException("Invalid failover entry: " + key);
            }
                
        }
                
    }

    private static synchronized long nextIID()
    {
        return ++_nextIID;
    }
    
    private void failover(AuthInfo info, boolean remove)
    {
        if (!_failoverEnabled || _failover == null || info == null) return;
        String serialized = remove? null : info.toSerialized();
        String id = "auth_" + info.token;
        try
        {
            _failover.write(id,  serialized);
        }
        catch (IOException x)
        {
            // what should we do?
            System.out.println("Failover failed: " + x.getMessage());
            _failoverEnabled = false;
        }
    }
	

    /**
     * Obtain a unsigned authentication token (with a new IID). 
     * This auth can be used by an authenticator app to sign other auth requests.
     * Note that this method is vulnerable to DDoS attacks, so it will need to change later.
     * Maybe an additional, semi-secret password?
     */
    public AuthResponse register(String name)
    {
        try
        {
            _authLock.writeLock().lock();
            AuthInfo auth = generateAuth(null);
            auth.name = name;
         
            return null;
        }
        finally
        {
            _authLock.writeLock().unlock();
        }
    }
    
	/**
	 * A check of the auth token that returns {@link AuthResponse} with the token status and the authenticated IID if available.
	 * Consumers should use this to confirm that their user's authentication is still valid
	 */
	public AuthResponse testAuthentication(Long requestingIdentityId, String authToken)
	{
		try
		{
			_authLock.readLock().lock();
			AuthInfo authInfo  = getAuth(authToken);
			if (authInfo == null || (authInfo.ownerIID != null && authInfo.ownerIID != requestingIdentityId))
			{
				return AuthResponse.NOTFOUND();
			}
			AuthResponse res = new AuthResponse(authInfo, null);
			if (authInfo.isExpired())
			{
			    res.authenticatedIID = null;
				res.status = AuthStatus.EXPIRED.name();
			}
			return res;
		}
		finally
		{
			_authLock.readLock().unlock();
		}
	}
	
	AuthInfo generateAuth(Long authenticatorIID)
	{
	    /*
		AuthInfo res = new AuthInfo();
		res.token = Utils.randomString(8);
		res.IID = null;//identityId;
		res.status = AuthStatus.NEW;
		res.authenticatorIID = authenticatorIID;
		res.expiryTime = new Date().getTime() + REQUEST_EXPIRY_SEC * 1000;
		
		try
		{
			_authLock.writeLock().lock();
			_authTable.put(res.token, res);
			failover(res, false);
		}
		finally
		{
			_authLock.writeLock().unlock();
		}
		return res;
		*/
	    return generateAuth(authenticatorIID, null, AuthStatus.NEW);
	}
	
    AuthInfo generateAuth(Long authenticatorIID, Long IID, AuthStatus status)
    {
        AuthInfo res = new AuthInfo();
        res.IID = IID;
        res.status = status;
        res.authenticatorIID = authenticatorIID;
        res.expiryTime = new Date().getTime() + AUTH_EXPIRY_SEC * 1000;
        
        try
        {
            _authLock.writeLock().lock();
            do
            {
                res.token = Utils.randomString(8);
            }
            while (_authTable.containsKey(res.token));
            _authTable.put(res.token, res);
            failover(res, false);
        }
        finally
        {
            _authLock.writeLock().unlock();
        }
        return res;
    }
    
	AuthInfo getAuth(String token)
	{
		if (Utils.isEmpty(token))
			return null;
		try
		{
			_authLock.readLock().lock();
			AuthInfo res = _authTable.get(token);
			return res == null? null : res.clone();
		}
		finally
		{
			_authLock.readLock().unlock();
		}
	}
	
	/**
	 * Do not use! This call is needed by SSOAuthRequestService to do read-and-update on auth records. 
	 */
	void doWriteLock()
	{
	    _authLock.writeLock().lock();
	}
	
    /**
     * Do not use! This call is needed by SSOAuthRequestService to do read-and-update on auth records. 
     */
    void doWriteUnlock()
    {
        _authLock.writeLock().unlock();
    }
    
	AuthInfo updateAuth(AuthInfo info)
	{
		if (Utils.isBlank(info.token))
		{
			return null;
		}
		try
		{
			_authLock.writeLock().lock();
			if (!_authTable.containsKey(info.token))
				return null;
			_authTable.put(info.token, info.clone());
			failover(info, false);
			return _authTable.get(info.token);
		}
		finally
		{
			_authLock.writeLock().unlock();
		}
	}
	
	boolean removeAuth(String token)
	{
		if (Utils.isEmpty(token))
			return false;
		try
		{
			_authLock.writeLock().lock();
			AuthInfo info = _authTable.remove(token);
			failover(info, true);
            return info!= null;
		}
		finally
		{
			_authLock.writeLock().unlock();
		}
	}

}
