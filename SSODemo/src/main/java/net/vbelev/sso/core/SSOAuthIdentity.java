package net.vbelev.sso.core;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import net.vbelev.utils.Utils;

/**
 * A table of identities (IIDs) registered or authenticated in this SSO. It provides
 * 1) conversion between internal IIDs and publicly visible tokens;
 * 2) access to the basic identity info (public name, authenticator IID, authenticator's key for this identity);
 * 
 * Note that it relies on the IIDs from the auth identity service, so there may be problems with the failover. 
 * 
 * The whole class is package-private, so its internals are mostly public.
 * 
 * 
 */
class SSOAuthIdentity
{
    /** Privately stored token info */
    private static class TokenInfo
    {
        /** The token */
        public String token;
        /** The internal IID */
        public long IID;
        /** Tokens can be self-issued (with no authenticator), which is useful for authenticators themselves. */
        public Long authenticatorIID;
        /**
         * The public (non-unique) name of the authenticated identity, used to mask 
         * the non-human-friendly authenticator key on the UI
         */
        public String name;
        /** The key (traditionally, the email) used by the authenticator to recognize the identify behind the IID.*/
        public String authenticatingKey;
        public long staleTime;
        public long expiryTime;
        
        public TokenInfo(String serialized) throws IOException
        {
            String[] fields = serialized.split(":");
            if (fields.length != 7)
                throw new IOException("Invalid serialized string");
            try
            {
                token = fields[0];
                IID = Utils.parseLong(fields[1]);
                authenticatorIID = Utils.parseLong(fields[2]);
                name = Utils.decode64(fields[3]);
                authenticatingKey = Utils.decode64(fields[4]);
                staleTime = Utils.parseLong(fields[5]);
                expiryTime = Utils.parseLong(fields[6]);
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
            sb.append(Utils.stringOrEmpty(IID));
            sb.append(":");
            sb.append(Utils.stringOrEmpty(authenticatorIID));
            sb.append(":");
            sb.append(Utils.encode64(name));
            sb.append(":");
            sb.append(Utils.encode64(authenticatingKey));
            sb.append(":");
            sb.append(Utils.stringOrEmpty(staleTime));
            sb.append(":");
            sb.append(Utils.stringOrEmpty(expiryTime));
            
            return sb.toString();
        }
        
        public boolean isExpired()
        {
            return expiryTime < new Date().getTime();
        }
                
    }
    
    public static class TokenResponse
    {
        /** Note that the returned token may be different from the requested, if the requested token is stale */
        public String token;
        public String authenticatorToken;
        public String name;
        public String key;
    }
    private final ReentrantReadWriteLock _infoLock = new ReentrantReadWriteLock();
    /**
     * The map of issued tokens to IIDs
     */
    private final Hashtable<String, TokenInfo> _tokenTable = new Hashtable<String, TokenInfo>();
    /**
     * The map of IIDs to the most recent token
     */
    private final Hashtable<Long, String> _iidTable = new Hashtable<Long, String>();
    /** 
     * The map of IIDs to the info records
     */
    private final Hashtable<Long, TokenInfo> _infoTable = new Hashtable<Long, TokenInfo>();
    
    public TokenInfo getTokenInfo(long IID, boolean createIfMissing)
    {
        try
        {
            _infoLock.readLock().lock();
            return _tokenTable.get(IID);
        }
        finally
        {
            _infoLock.readLock().unlock();
        }
    }
}
