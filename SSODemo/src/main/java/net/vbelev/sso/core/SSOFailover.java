package net.vbelev.sso.core;

import java.util.*;

import org.apache.tomcat.jakartaee.commons.io.Charsets;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.Path;

import net.vbelev.utils.Utils;

/**
 * An instance of this class (one per a list of managed entities)
 */
public class SSOFailover
{
    private static final String DEFAULT_FILE_PREFIX = "data";
    private static final String DEFAULT_FILE_SUFFIX = ".txt";
    private static final int DEFAULT_ROW_LIMIT = 10;
    
    private final Path _rootFolder;
    private final String _filePrefix;
    private final String _fileSuffix;
    private final int _rowLimit;
    private final TreeMap<String, Integer> _idMap;
    private int _fileIndex = 0;
    private File _file;
    /**
     * A primitive protection against modification by other applications
     */
    private long _fileLastModified;
    /**
     * The number of rows writted into the current log file
     */
    private int _rowCount = 0;
    
    public SSOFailover(String rootFolder, String filePrefix)
    {
        this(rootFolder, filePrefix, DEFAULT_FILE_SUFFIX, DEFAULT_ROW_LIMIT);
    }
    
    public SSOFailover(String rootFolder, String filePrefix, String fileSuffix, int rowLimit)
    {
        try
        {
            _filePrefix = Utils.isBlank(filePrefix) ? DEFAULT_FILE_PREFIX : filePrefix;
            _fileSuffix = fileSuffix == null? DEFAULT_FILE_SUFFIX : fileSuffix;
            _rowLimit = rowLimit;
            _idMap = new TreeMap<String, Integer>();
            
            File f = Path.of(rootFolder).toFile();
            if (!f.exists())
            {
                if (!f.mkdirs())
                {
                    throw new IllegalArgumentException("The failover root folder " + rootFolder + " could not be created");
                }
            }
            else if (!f.isDirectory())
            {
                throw new IllegalArgumentException("The failover root folder path " + rootFolder + " is not a directory");
            }
            
            _rootFolder = Path.of(rootFolder).toRealPath();
            /*
            for (File f2 : f.listFiles((dir, name) -> (name.startsWith(_filePrefix) && name.endsWith(_fileSuffix))))
            {
                String f2Name = f2.getName();
                //if (!f2Name.startsWith(_filePrefix) || !f2Name.endsWith(_fileSuffix))
                //    continue;
                int f2Index = indexFromName(f2Name);
                if (f2Index <= 0)
                    throw new IllegalArgumentException("Invalid file in the failover root folder: " + f2Name);
                if (f2Index > _fileIndex)
                    _fileIndex = f2Index;
            }*/
        }
        catch (IOException ie)
        {
            throw new IllegalArgumentException("The failover root folder " + rootFolder + " is not accessible");
        }
        catch (SecurityException se)
        {
            throw new IllegalArgumentException("The failover root folder " + rootFolder + " is not accessible");
        }
    }

    /**
     * Only useful for automated tests
     */
    public int getFileIndex()
    {
        return _fileIndex;
    }

    /**
     * Only useful for automated tests
     */
    public int getRowCount()
    {
        return _rowCount;
    }
    
    private int indexFromName(String name)
    {
        if (name.length() < _filePrefix.length() + _fileSuffix.length())
            return 0;
        try        
        {
            return Integer.parseInt(name.substring(_filePrefix.length(), name.length() - _fileSuffix.length()));
        }
        catch (NumberFormatException x)
        {
            return 0;
        }
    }
    
    /**
     * Move to the next failover file: increment _fileIndex and create a new file for writing failover records. 
     * @throws IOException
     */
    private synchronized void startFile() throws IOException
    {                
        flushFiles();
        File nextFile = _rootFolder.resolve(_filePrefix + (_fileIndex + 1) + _fileSuffix).toFile();
        if (nextFile.exists())
        {
            throw new IOException("Unexpected file in the failover folder: " + nextFile.getAbsolutePath());            
        }
        _fileIndex++;
        boolean isCreated;
        try
        {
            isCreated = nextFile.createNewFile();
            SecurityManager security = System.getSecurityManager();
            boolean cw = nextFile.canWrite();
            System.out.println("file " + nextFile.getName() + " can write: " + cw);
        }
        catch (SecurityException se)
        {
            isCreated = false;            
        }
        if (!isCreated)
        {
            throw new IOException("Failed to create the file in the failover folder: " + nextFile.getAbsolutePath());            
        }
        _file = nextFile;
        _fileLastModified = _file.lastModified();
        _rowCount = 0;
        
    }

    /**
     * Check which existing failover files are obsolete (all their records are overwritten or deleted in later files)
     * and delete them.
     * @throws IOException
     */
    private synchronized void flushFiles() throws IOException
    {
        int minIndex = Integer.MAX_VALUE; // the Collections.min() can be used, too
        for(Integer val : _idMap.values())
        {
            if (val < minIndex)
                minIndex = val;
        }
        File[] allFiles = _rootFolder.toFile().listFiles((dir, name) -> (name.startsWith(_filePrefix) && name.endsWith(_fileSuffix)));
        for (File f : allFiles)
        {
            String fName = f.getName();
            //if (!f2Name.startsWith(_filePrefix) || !f2Name.endsWith(_fileSuffix))
            //    continue;
            int fIndex = indexFromName(fName);
            if (fIndex < minIndex)
                f.delete();
        }        
        File[] remainingFiles = _rootFolder.toFile().listFiles((dir, name) -> (name.startsWith(_filePrefix) && name.endsWith(_fileSuffix)));
        if (remainingFiles.length == 0)
        {
            _fileIndex = 0;
            _rowCount = 0;
            _idMap.clear();
            _file = null;
            _fileLastModified = 0;
        }
    }
    
    /**
     * For debugging, the string is saved and loaded without any changes, unprotected.
     * For production, it will be better to encrypt or sign it in some way.
     * @param content
     * @return
     */
    protected String prepareContent(String content)
    {
        if (content == null)
            return "";
        if (content.indexOf('\n') > 0)
        {
            throw new IllegalArgumentException("content must be a single line");
        }
        return content;
    }
    
    /**
     * For debugging, the content string is loaded without any changes.
     * For production, it will be decoded or decrypted (see {@link prepareContent}.
     * If the converter is not happy with the content string, it should throw an IOException
     * and fail the whole recovery process. 
     */
    protected String restoreContent(String contentLine) throws IOException
    {
        return contentLine;
    }
    
    /**
     * The content must be a single line, and the id must be a token (single line, no whitespace).
     * For now, the method is a slow, single-threaded write. I'll probably change it later.
     * @param content When the null or empty string is provided, it is interpreted as "delete object".
     */    
    public synchronized void write(String id, String content) throws IOException
    {
        if (Utils.isEmpty(id) || id.indexOf(' ') >= 0 || id.indexOf('\n') >= 0)
        {
            throw new IllegalArgumentException("id must be a token");
        }
        String preparedContent = prepareContent(content);

        if (_rowCount <= 0 || _rowCount >= _rowLimit)
            startFile();

        if (_file.lastModified() != _fileLastModified)
        {
            throw new IOException("Failover modified by another application");
        }
        FileWriter writer = new FileWriter(_file, true);
        writer.write(id + " " + preparedContent + "\n");
        writer.close();
        _fileLastModified = _file.lastModified();
        _rowCount++;
        
        if (Utils.isEmpty(content))
        {
            _idMap.remove(id);
        }
        else
        {
            _idMap.put(id, _fileIndex);
        }
    }
 
    /**
     * Call this method once to restore the inner counters and to get a dictionary of all restored entities.  
     * Note that the returned dictionary is not ordered in any particular way.
     */
    public synchronized Map<String, String> restore() throws IOException
    {
        if (!_idMap.isEmpty())
            throw new IOException("The failover instance is already in use");
        
        Hashtable<String, String> res = new Hashtable<String, String>();

        File[] allFiles = _rootFolder.toFile().listFiles((dir, name) -> (name.startsWith(_filePrefix) && name.endsWith(_fileSuffix)));
        Arrays.sort(allFiles, new Comparator<File>()
        {
            @Override
            public int compare(File o1, File o2)
            {
                int x = indexFromName(o1.getName());
                int y = indexFromName(o2.getName());
                // TODO Auto-generated method stub
                return (x < y) ? -1 : ((x == y) ? 0 : 1);
            }
        });
        _fileIndex = 0;
        for (File f : allFiles)
        {
            String fName = f.getName();
            //if (!f2Name.startsWith(_filePrefix) || !f2Name.endsWith(_fileSuffix))
            //    continue;
            int fIndex = indexFromName(fName);
            if (fIndex <= 0)
            {
                throw new IOException("Invalid file in the failover root folder: " + fName);
            }
            if (fIndex > _fileIndex)
                _fileIndex = fIndex;

            _rowCount = 0;
            _file = f;
            BufferedReader breader = new BufferedReader(new FileReader(f, StandardCharsets.UTF_8));
            String line;
            String id;
            String content;
            while ((line = breader.readLine()) != null) {
                _rowCount++;
                int space = line.indexOf(' ');
                id = space < 0? line : line.substring(0, space);
                content = space < 0? "" : restoreContent(line.substring(space + 1));
                
                if (Utils.isEmpty(content))
                {
                    _idMap.remove(id);
                    res.remove(id);
                }
                else
                {
                    _idMap.put(id, fIndex);
                    res.put(id, content);
                }
            }
            breader.close();
        }
        if (_file != null)
            _fileLastModified = _file.lastModified();
        else
            _fileLastModified = 0;
        return res;
    }
     
    /**
     * Delete all existing failover files and clear the internal counters.
     */
    public synchronized void reset()
    {
        File[] allFiles = _rootFolder.toFile().listFiles((dir, name) -> (name.startsWith(_filePrefix) && name.endsWith(_fileSuffix)));
        for (File f : allFiles)
        {
            f.delete();
        }   
        _fileIndex = 0;
        _rowCount = 0;
        _idMap.clear();
        _file = null;
        _fileLastModified = 0;
    }
}
