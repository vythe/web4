package net.vbelev.sso.core;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.vbelev.utils.Utils;

/**
 * This service stores and handles auth requests: generate a request, complete a request and such.
 * Note that it uses a {@link SSOAuthProvider} instance to work with auth records.
 */
public class SSOAuthRequestProvider
{
    private static final long REQUEST_EXPIRY_SEC = 100;
    
    public static enum AuthRequestStatus
    {
        NOTFOUND,
        NEW,
        INPROGRESS,
        READY,
        REDEEMING,
        REDEEMED,
        CANCELLED
    }

    /**
     * Private info about auth requests, stored in the list {@link SSOAuthProvider#_requestTable request table}
     */
    static class AuthRequestInfo
    {
        public String requestToken;

        /**
         * IID of the application that requested an auth. 
         * If it is null, the system will not send success/fail notifications 
         * and will not restrict auth status checks to the requesting application only.  
         */
        public Long requesterIID;
            
        public Long authenticatorIID;
        /**
         * An auth request may be to confirm a particular user (e.g. to refresh an expired token),
         * in that case the request will include the requested IID.
         * Note that a request record will not contain the authenticated user's IID even after the auth is completed.
         */
        public Long requestedIID;
        
        /**
         * This auth token cross-links the request with the authentication
         */
        public String authToken;
        public long expiryTime;
        public AuthRequestStatus status = AuthRequestStatus.NEW;
        
        public AuthRequestInfo()
        {
        }
        
        public AuthRequestInfo(String serialized) throws IOException
        {
            String[] fields = serialized.split(":");
            if (fields.length != 7)
                throw new IOException("Invalid serialized string");
            try
            {
                requestToken = fields[0];
                expiryTime = Utils.parseLong(fields[1]);
                requesterIID = Utils.parseLong(fields[2]);
                authenticatorIID = Utils.parseLong(fields[3]);
                requestedIID = Utils.parseLong(fields[4]);
                authToken = fields[5];
                status = AuthRequestStatus.valueOf(fields[6]);
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
            sb.append(requestToken);
            sb.append(":");
            sb.append(expiryTime);
            sb.append(":");
            sb.append(Utils.stringOrEmpty(requestedIID));
            sb.append(":");
            sb.append(Utils.stringOrEmpty(authenticatorIID));
            sb.append(":");
            sb.append(Utils.stringOrEmpty(requestedIID));
            sb.append(":");
            sb.append(authToken);
            sb.append(":");
            sb.append(status.name());
            
            return sb.toString();
        }
        
        
        public boolean isActive()
        {
            return !isExpired() && Utils.InList(status, AuthRequestStatus.NEW, AuthRequestStatus.INPROGRESS, AuthRequestStatus.READY, AuthRequestStatus.REDEEMING);
        }
        
        public boolean isExpired()
        {
            return expiryTime < new Date().getTime();
        }
        
        public AuthRequestInfo clone()
        {
            AuthRequestInfo res = new AuthRequestInfo();
            res.requesterIID = requesterIID;
            res.authenticatorIID = authenticatorIID;
            res.requestedIID = requestedIID;
            res.requestToken = requestToken;
            res.authToken = authToken;
            res.expiryTime = expiryTime;
            res.status = status;
                    
            return res;
        }
        
        public AuthRequestResponse buildResponse()
        {
            AuthRequestResponse res = new AuthRequestResponse();
            res.expiryTime = expiryTime;
            res.requestedIID = requestedIID;
            res.requesterIID = requesterIID;
            res.requestToken = requestToken;
            res.status = status.name();
            
            return res;
        }
    }
    
    /**
     * Public response to an auth request. It is needed by an auth application to complete the auth 
     */
    public static class AuthRequestResponse
    {
        //public static final AuthRequestResponse NOTFOUND = new AuthRequestResponse(null, AuthRequestStatus.NOTFOUND.name());
        
        public String requestToken;
        public String status;
        /**
         * Do i need any identity ids here?
         */
        public Long requesterIID;
        /**
         * This is auth restriction: if set, the auth will confirm this identity or nothing
         */
        public Long requestedIID;
        
        public Long expiryTime;
        
        public AuthRequestResponse()
        {
        }
        
        public AuthRequestResponse(AuthRequestStatus status)
        {
            this.status = status.name();
        }
        
        public static AuthRequestResponse NOTFOUND()
        {
            return new AuthRequestResponse(AuthRequestStatus.NOTFOUND);
        }
        
    }

    private final ReentrantReadWriteLock _requestLock = new ReentrantReadWriteLock();
    private final Hashtable<String, AuthRequestInfo> _requestTable = new Hashtable<String, AuthRequestInfo>();

    private final SSOAuthProvider _authService;
    private final SSOFailover _failover; 
    private boolean _failoverEnabled;
    
    public SSOAuthRequestProvider(SSOAuthProvider authService)
    {
        _authService = authService;
        _failover = null;
        _failoverEnabled = false;
    }
    
    public SSOAuthRequestProvider(SSOAuthProvider authService, String failoverFolder) throws IOException
    {
        _authService = authService;
        //_failover = new SSOFailover(Path.of(homeFolder, "SSOFailoverTest").toAbsolutePath().toString(), "simple", null, 5);
        _failover = new SSOFailover(failoverFolder, "request", null, 5);
        _failoverEnabled = true;
        Map<String, String> recoveredAuth = _failover.restore();
        for (Map.Entry<String, String> entry : recoveredAuth.entrySet())
        {
            String key = entry.getKey();
            if (key.startsWith("req_"))
            {
                AuthRequestInfo req = new AuthRequestInfo(entry.getValue());
                _requestTable.put(req.requestToken, req);                        
            }
            /*
            else if (key.startsWith("auth_"))
            {
                AuthInfo auth = new AuthInfo(entry.getValue());
                _authTable.put(auth.token, auth);
            }*/
            else               
            {
                throw new IllegalArgumentException("Invalid failover entry: " + key);
            }
                
        }
    }
    
    private void failover(AuthRequestInfo request, boolean remove)
    {
        if (!_failoverEnabled || _failover == null || request == null) return;
        String serialized = remove? null : request.toSerialized();
        String id = "req_" + request.requestToken;
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
     * Create a request token that can be redeemed to a normal auth token, already approved.
     * The returned {@link AuthResponse} will provide a request token, status and an expiry time, everything else is blank.
     * Note that the issuer application does not need to confirm its "right" for the userIID. It can issue a token for any user.
     * @param authenticatorIID This application issues an auth
     * @param userIID the authenticated user's IID to transfer
     * @param receiverIID This application will convert the created request token into a valid auth token for the userIID.
     * @return
     */
    public SSOAuthProvider.AuthResponse transferAuth(Long authenticatorIID, long userIID, Long receiverIID)
    {
        try
        {
            _requestLock.writeLock().lock();
            
            AuthRequestInfo requestInfo = generateRequest(receiverIID, null, authenticatorIID, userIID, AuthRequestStatus.REDEEMING);
            SSOAuthProvider.AuthResponse resp = new SSOAuthProvider.AuthResponse(requestInfo);
            return resp;
        }
        finally
        {
            _requestLock.writeLock().unlock();
        }
    }
    
    /**
     * Try to convert your request token into an auth token.
     * The result is the same AuthResponse as returned by {@link testAuthentication}, plus a non-empty auth token.
     * If the authentication is not complete,all fields except status will be blank.
     * Note that an attempt to redeem a token that is already redeemed will destroy the authentication.
     * 
     * When authentication was initiated by a third party, you get a request token 
     * generated by them and convert it into a request from yourself.
     * Not sure this is needed.
     */
    public SSOAuthProvider.AuthResponse redeemRequestToken(Long requesterIID, String requestToken)
    {
        AuthRequestInfo requestInfo = getRequestInfo(requestToken);
        if (requestInfo == null)
            return SSOAuthProvider.AuthResponse.NOTFOUND();
        
        if (requestInfo.isExpired())
        {
            removeRequest(requestToken);
            return SSOAuthProvider.AuthResponse.NOTFOUND();
        }
        
        if (requestInfo.requesterIID != null && requestInfo.requesterIID != requesterIID)
        {
            return SSOAuthProvider.AuthResponse.NOTFOUND();
        }
        
        if (requestInfo.status == AuthRequestStatus.NEW || requestInfo.status == AuthRequestStatus.INPROGRESS)
        {
            return new SSOAuthProvider.AuthResponse(SSOAuthProvider.AuthStatus.NOTREADY);
        }
        
        try
        {
            _requestLock.writeLock().lock();
    
            if (requestInfo.status == AuthRequestStatus.REDEEMED) // && !Utils.isEmpty(requestInfo.authToken))
            {
                _authService.removeAuth(requestInfo.authToken);
                // we should notify the requesting app that their token was redeemed twice.
                return SSOAuthProvider.AuthResponse.NOTFOUND();         
            }
            
            SSOAuthProvider.AuthInfo auth = _authService.generateAuth(requestInfo.authenticatorIID, 
                    requestInfo.requestedIID, 
                    requestInfo.requestedIID  == null? SSOAuthProvider.AuthStatus.NEW : SSOAuthProvider.AuthStatus.ACTIVE
            );
            /*
            auth.IID = requestInfo.requestedIID;
            auth.status = auth.IID == null? SSOAuthService.AuthStatus.NEW : SSOAuthService.AuthStatus.ACTIVE;
            authService.updateAuth(auth);
            */
            requestInfo.authToken = auth.token;
            requestInfo.status = AuthRequestStatus.REDEEMED;
            updateRequestInfo(requestInfo);
    
            SSOAuthProvider.AuthResponse res = new SSOAuthProvider.AuthResponse(auth, null); //request.requestToken, request.status.name());
            return res;
        }
        finally
        {
            _requestLock.writeLock().unlock();
        }
    }

    /**
     * This is the entry point: the application registered as {@code requestingIdentityId}
     * wants to get an authenticated user or to confirm that the user {@code requestedIID} is himself.
     * If the requesting Id is null, they will not be notified of the results, but they can still
     * use the request token to check.
     * @param requestingIdentityId IID of the application requesting an auth
     * @param authenticatorIID IID of the application that should perform the auth (normally, null)
     * @param requestedIID when not null, the auth will be restricted to this user
     */
    public SSOAuthProvider.AuthResponse requestAuth(Long requestingIdentityId, Long authenticatorIID, Long requestedIID)
    {
        SSOAuthProvider.AuthInfo auth = _authService.generateAuth(authenticatorIID);
        AuthRequestInfo request = generateRequest(requestingIdentityId, auth.token, authenticatorIID, requestedIID); 
        
        if (request == null)
            return SSOAuthProvider.AuthResponse.NOTFOUND();
        
        if (request.status == AuthRequestStatus.NEW)
        {
            // notify the auth application here. It may immediately update the request,
            // and i need to do something about it
        }
        SSOAuthProvider.AuthResponse res = new SSOAuthProvider.AuthResponse(auth, request);
        
        return res;
    }

    /**
     * This is called by an auth application to set the request status to INPROGRESS, complete the auth or cancel the request 
     * @param authenticatorIID
     * @param requestToken
     * @param identityId - if it is null, the status is set to NOTREADY; otherwise to ACTIVE
     * @return
     */
    public AuthRequestResponse updateRequestStatus(Long authenticatorIID, String requestToken, Long identityId, String name, String authenticatingKey)
    {
        try
        {
            _requestLock.writeLock().lock();
            
            AuthRequestInfo requestInfo = getRequestInfo(requestToken);
            if (requestInfo == null 
                || (requestInfo.authenticatorIID != null && requestInfo.authenticatorIID != authenticatorIID)
            )
            {
                return AuthRequestResponse.NOTFOUND();
            }
        
            _authService.doWriteLock();
            SSOAuthProvider.AuthInfo authInfo = _authService.getAuth(requestInfo.authToken);
            if (authInfo == null || authInfo.status == SSOAuthProvider.AuthStatus.EXPIRED)
            {
                removeRequest(requestToken);
                return AuthRequestResponse.NOTFOUND();
            }
            else if (authInfo.status == SSOAuthProvider.AuthStatus.ACTIVE)
            {
                // Double authentication! Abort! Notify!
                _authService.removeAuth(authInfo.token);
                removeRequest(requestToken);
                return AuthRequestResponse.NOTFOUND();
            }
                        
            // We are under a write lock, so we hope to update the records safely
            authInfo.status = identityId == null? SSOAuthProvider.AuthStatus.NOTREADY : SSOAuthProvider.AuthStatus.ACTIVE;
            authInfo.IID = identityId;
            authInfo.name = name;
            authInfo.authenticatingKey = authenticatingKey;
            authInfo = _authService.updateAuth(authInfo);
            if (authInfo == null)
            {
                return AuthRequestResponse.NOTFOUND(); // something went wrong?
            }
            requestInfo.status = authInfo.status == SSOAuthProvider.AuthStatus.ACTIVE ? AuthRequestStatus.READY : AuthRequestStatus.INPROGRESS;
            updateRequestInfo(requestInfo);
            AuthRequestResponse res = new AuthRequestResponse();
            res.status = authInfo.status.name();
            res.requestToken = requestToken;
            
            return res;
        }
        finally
        {
            _requestLock.writeLock().unlock();
            _authService.doWriteUnlock();
        }
    }
    
    /**
     * The auth app can cancel the request when the user fails to authenticate. 
     * There is no difference between user providing a wrong password or choosing to walk away.
     * The requesting app cannot cancel a request in this way, but they can cancel the auth token,
     * and that will cancel the request as well. 
     * @param authIdentityId
     * @param requestToken
     * @return 
     */
    public AuthRequestResponse cancelRequest(Long authIdentityId, String requestToken)
    {
        try
        {
            _requestLock.writeLock().lock();
            
            AuthRequestInfo requestInfo = getRequestInfo(requestToken);
            if (requestInfo == null 
                || (requestInfo.authenticatorIID != null && requestInfo.authenticatorIID != authIdentityId)
            )
                return AuthRequestResponse.NOTFOUND();
        
            _authService.doWriteLock();
            SSOAuthProvider.AuthInfo authInfo = _authService.getAuth(requestInfo.authToken);
            if (authInfo == null || authInfo.status == SSOAuthProvider.AuthStatus.EXPIRED)
            {
                removeRequest(requestToken);
                return AuthRequestResponse.NOTFOUND();
            }
            else if (authInfo.status == SSOAuthProvider.AuthStatus.ACTIVE)
            {
                // Double authentication! Abort! Notify!
                _authService.removeAuth(authInfo.token);
                removeRequest(requestToken);
                return AuthRequestResponse.NOTFOUND();
            }
                        
            // should notify the requester here
            _authService.removeAuth(authInfo.token);
            removeRequest(requestToken);
            AuthRequestResponse res = new AuthRequestResponse(AuthRequestStatus.CANCELLED);
            res.requestToken = requestToken;
            return res;             
        }
        finally
        {
            _authService.doWriteUnlock();
            _requestLock.writeLock().unlock();
        }       
    }
    
    /**
     * The auth app needs information about the request to complete the auth.
     * Note that the info is available to the designated auth app, not to the application that generated the request
     */
    public AuthRequestResponse getRequestInfo(Long requestingIdentityId, String requestToken)
    {
        AuthRequestInfo requestInfo = getRequestInfo(requestToken);
        if (requestInfo == null || (requestInfo.authenticatorIID != null && requestInfo.authenticatorIID != requestingIdentityId))
            return AuthRequestResponse.NOTFOUND();
        
        SSOAuthProvider.AuthInfo authInfo  = _authService.getAuth(requestInfo.authToken);        
        if (requestInfo.status == AuthRequestStatus.READY && authInfo == null)
        {
            removeRequest(requestToken); // something bad happened to the auth token
            return AuthRequestResponse.NOTFOUND();
        }

        AuthRequestResponse res = requestInfo.buildResponse();
        return res;
    }
    
    private AuthRequestInfo generateRequest(Long requestingIdentityId, String authToken, Long authIdentityId, Long identityId)
    {
        return generateRequest(requestingIdentityId, authToken, authIdentityId, identityId, AuthRequestStatus.NEW);
    }
    
    /**
     * 
     * @param requestingIdentityId Application that needs to auth a user (may be null)
     * @param authToken The main auth token (will be null for authenticatio transfer requests)
     * @param authIdentityId
     * @param identityId
     * @return
     */
    private AuthRequestInfo generateRequest(Long requestingIdentityId, String authToken, Long authIdentityId, Long identityId, AuthRequestStatus status)
    {
        try
        {
            _requestLock.writeLock().lock();
            AuthRequestInfo res = new AuthRequestInfo();
            res.requestToken = Utils.randomString(6);
            res.authToken = authToken;
            res.authenticatorIID = authIdentityId;
            res.requesterIID = requestingIdentityId;
            res.requestedIID = identityId;
            res.status = status;
            res.expiryTime = new Date().getTime() + REQUEST_EXPIRY_SEC * 1000;
            
            _requestTable.put(res.requestToken,  res);
            failover(res, false);
            
            return res;
        }
        finally
        {
            _requestLock.writeLock().unlock();
        }
    }
    
    private AuthRequestInfo getRequestInfo(String requestToken)
    {
        if (Utils.isEmpty(requestToken))
            return null;
        try
        {
            _requestLock.readLock().lock();
            AuthRequestInfo res = _requestTable.get(requestToken);
            return res == null? null : res.clone();
        }
        finally
        {
            _requestLock.readLock().unlock();
        }
    }
    
    private AuthRequestInfo updateRequestInfo(AuthRequestInfo info)
    {
        if (Utils.isBlank(info.requestToken))
            return null;
        try
        {
            _requestLock.writeLock().lock();
            if (!_requestTable.containsKey(info.requestToken))
                return null;
            _requestTable.put(info.requestToken, info.clone());
            failover(info, false);
            return _requestTable.get(info.requestToken);
        }
        finally
        {
            _requestLock.writeLock().unlock();
        }
    }

    private boolean removeRequest(String requestToken)
    {
        if (Utils.isEmpty(requestToken))
            return false;
        try
        {
            _requestLock.writeLock().lock();
            AuthRequestInfo info = _requestTable.remove(requestToken);
            failover(info, true);
            return info != null;
        }
        finally
        {
            _requestLock.writeLock().unlock();
        }
    }
    
}
