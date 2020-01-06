package net.vbelev.web4.utils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.*;
import java.util.*;

import org.w3c.dom.*;

import javax.xml.bind.*;
import javax.xml.bind.annotation.*;
//import javax.xml.bind.annotation.*;
import javax.xml.namespace.QName;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.*;


public class XMLiser
{
	@XmlRootElement(name = "object_wrap")
	public static class ObjectWrapper
	{
		@XmlAttribute(name = "class_name")
		public String className;
		@XmlElement(name = "content")
		public String contentString;
		
		public ObjectWrapper()
		{
			className = null;
			contentString = null;
		}
		
		public ObjectWrapper(Serializable s)
		{
			setObject(s);
		}
		public Object getObject()
		{
			if (className == null || contentString == null)
			{
				return null;
			}
			else if (className.equals(String.class.getName()))
			{
				return contentString;
			}
			else
			{
				byte[] objBytes = stringToBytes(contentString);
				try(ByteArrayInputStream bs = new ByteArrayInputStream(objBytes))
				{
				ObjectInputStream is = new ObjectInputStream(bs);
				Object res = is.readObject();
				is.close();
				return res;
				}
				catch(ClassNotFoundException x)
				{
					throw new IllegalArgumentException("ObjectWrapper.getObject failed", x);
				}
				catch(IOException x)
				{
					throw new IllegalArgumentException("ObjectWrapper.getObject failed", x);
				}
			}
				
		}
		public void setObject(Serializable s)
		{
			if (s == null)
			{
				className = null;
				contentString = null;
			}
			else if (s instanceof String)
			{
				className = s.getClass().getName();
				contentString = (String)s;
			}
			else
			{
				className = s.getClass().getName();
				try(ByteArrayOutputStream bs = new ByteArrayOutputStream())
				{
					ObjectOutputStream os = new ObjectOutputStream(bs);
					os.writeObject(s);
					os.flush();
					
					contentString = stringFromBytes(bs.toByteArray());
				}
				catch (IOException x)
				{
					throw new IllegalArgumentException("ObjectWrapper.setObject failed", x);
				}
				finally
				{
				}
			}
		}
	}

	public static final Charset XMLiserCharset = StandardCharsets.UTF_8;
	protected static final CharsetDecoder XMLiserCharsetDecoder = XMLiserCharset.newDecoder();
	protected static final CharsetEncoder XMLiserCharsetEncoder = XMLiserCharset.newEncoder();
	
	public final Class[] Classes;
	public final JAXBContext Context;
	protected Marshaller contextMarshaller;
	protected Unmarshaller contextUnmarshaller;
	
	public static String stringFromBytes(byte[] bytes)
	{
		if (bytes == null) return null;
		if (bytes.length == 0) return "";
		try
		{
			return XMLiserCharsetDecoder.decode(
					ByteBuffer.wrap(
							bytes
					)
			).toString();
		}
		catch (Exception x)
		{
			throw new IllegalArgumentException("Failed to convert bytes to " + XMLiserCharset.toString());
		}
	}
	
	public static byte[] stringToBytes(String s)
	{
		if (s == null) return null;
		if (s.length() == 0) return new byte[0];
		try
		{
			return XMLiserCharsetEncoder.encode(
					CharBuffer.wrap(
							s.toCharArray()
					)
			).array();
		}
		catch (Exception x)
		{
			throw new IllegalArgumentException("Failed to convert bytes to " + XMLiserCharset.toString());
		}
	}

	public XMLiser()
	{
		try
		{
			Classes = new Class[0];
			Context = JAXBContext.newInstance(ObjectWrapper.class);
			contextMarshaller = Context.createMarshaller();
			contextMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			
			contextUnmarshaller = Context.createUnmarshaller();
		}
		catch  (Exception x)
		{
			throw new IllegalArgumentException("failed to initialize JAXB", x);
		}
	}
	
	public XMLiser(Class... classesToBeBound)
	{
		try
		{
			ArrayList<Class> classes = new ArrayList<Class>(classesToBeBound.length + 1);
			classes.add(ObjectWrapper.class);
			for (Class c : classesToBeBound)
			{
				classes.add(c);
			}
			Classes = classesToBeBound.clone();
			Context = JAXBContext.newInstance(Classes, null);
			contextMarshaller = Context.createMarshaller();
			contextMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			contextUnmarshaller = Context.createUnmarshaller();
		}
		catch  (Exception x)
		{
			throw new IllegalArgumentException("failed to initialize JAXB", x);
		}
	}

	public static List<Class> getAllClasses(ClassLoader cc, String packageName)
	{
		ArrayList<Class> res = new ArrayList<Class>();
		try
		{
			if (cc == null)
			{
				cc = Thread.currentThread().getContextClassLoader();
			}
		Enumeration<java.net.URL> allfiles = cc.getResources(packageName.replaceAll("\\.", "/") + "/*");
		while (allfiles.hasMoreElements())
		{
			File fElem = new File(allfiles.nextElement().getFile());
			if (fElem.isFile() && fElem.getName().endsWith(".class"))
			{
				try
				{
				Class<?> c = cc.loadClass(packageName + "." + fElem.getName().replaceFirst("\\.class$", ""));
				res.add(c);
				
				}
				catch (ClassNotFoundException cx)
				{
					System.out.println("getAllClasses failed on " + packageName + " - " + fElem.getName());
				}
			}
		}
		}
		catch (IOException x)
		{
			throw new IllegalArgumentException("Failed to read package " + packageName, x);
		}
		return res;
	}
	
	public XMLiser(String... packagesToBeBound)
	{
		try
		{
			String allNames = String.join(":", packagesToBeBound);
			Context = JAXBContext.newInstance(allNames, null );
			contextMarshaller = Context.createMarshaller();
			contextMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			contextUnmarshaller = Context.createUnmarshaller();
			
			//this.getClass().getClassLoader().
			List<Class> allClasses = new ArrayList<Class>();
			for (String p : packagesToBeBound)
			{
				allClasses.addAll(XMLiser.getAllClasses(null, p));
			}
			Classes = (Class[])allClasses.toArray();
		}
		catch  (Exception x)
		{
			throw new IllegalArgumentException("failed to initialize JAXB", x);
		}
	}
	
	public void toXML(OutputStream out, Object o)
	{
		try
		{
			if (o == null)
			{
				throw new IllegalArgumentException("argument is null");
				/* can't marshall nulls?
				ObjectWrapper ow = new ObjectWrapper();
				contextMarshaller.marshal(ow, out);
				return;
				*/
			}
			
			for (Class c : Classes)
			{
				if (c.isInstance(o))
				{
					contextMarshaller.marshal(o, out);
					return;
				}
			}
			
			if (o instanceof Serializable)
			{
				ObjectWrapper ow = new ObjectWrapper((Serializable)o);
				contextMarshaller.marshal(ow, out);
				return;
				
			}
		}
		catch  (Exception x)
		{
			throw new IllegalArgumentException("failed to serialize object to XML", x);
		}
	}
	
	public <T>void toXML(OutputStream out, T o, String nodeName)
	{
		if (nodeName == null || nodeName.length() == 0)
		{
			toXML(out, o);
			return;
		}
		try
		{
			if (o == null)
			{
				throw new IllegalArgumentException("argument is null");
				/* can't marshall nulls?
				ObjectWrapper oo = new ObjectWrapper();
				@SuppressWarnings("unchecked")
				JAXBElement<ObjectWrapper> elem = new JAXBElement<ObjectWrapper>( new QName(null, nodeName), ObjectWrapper.class, oo);
				contextMarshaller.marshal(elem, out);
				*/
			}
			else
			{
				@SuppressWarnings("unchecked")
				JAXBElement<T> elem = new JAXBElement<T>( new QName(null, nodeName), (Class<T>)o.getClass(), o);
				contextMarshaller.marshal(elem, out);
			}
		}
		catch (Exception x)
		{
			throw new IllegalArgumentException("failed to serialize object to XML with node " + nodeName, x);
		}
	}
	
	
	@SuppressWarnings("unchecked")
	public <T>T fromXML(Class<T> elementClass, InputStream xml)
	{
		if (xml == null) return null;
		
		//lock.lock();
		try
		{
		//StringReader tReader = new StringReader(xml);

			StreamSource ss = new StreamSource( xml);
		
			for (Class c : Classes)
			{
				if (elementClass.isAssignableFrom(c))
				{
					return contextUnmarshaller.unmarshal(ss, elementClass).getValue();
				}
			}
			ObjectWrapper ow = contextUnmarshaller.unmarshal(ss, ObjectWrapper.class).getValue();
			
			Object o = ow.getObject();
			if (o == null)
			{
				return null;
			}
			return (T)o; // this will throw a class cast exception
		
		}
		catch (Exception x)
		{
			throw new IllegalArgumentException("Unmarshaller", x);
		}
		finally
		{
			//lock.unlock();
		}
	}
	
	public Document streamToDocument(InputStream is)
	{
		try
		{
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        DocumentBuilder docBuilder = dbf.newDocumentBuilder();
	        
	        return docBuilder.parse(is);
		}
		catch (Exception x)
		{
			throw new IllegalArgumentException("streamToDocument", x);
		}
	}
	
	public OutputStream streamFromDocument(Document doc, OutputStream os)
	{
		try
		{
		//DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        //DocumentBuilder docBuilder = dbf.newDocumentBuilder();

			if (os == null)
			{
				os = new ByteArrayOutputStream();
			}
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer docTransformer = tf.newTransformer();
        docTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        docTransformer.transform(new DOMSource(doc.getDocumentElement()), new StreamResult(os) );
        
        return os;
		}
		catch (Exception x)
		{
			throw new IllegalArgumentException("streamToDocument", x);
		}
	}
}
