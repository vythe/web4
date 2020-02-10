package net.vbelev.web4;

import java.util.*;

import org.bson.BsonArray;
import org.bson.BsonValue;
import org.bson.Document;

import java.io.*;
import java.nio.file.Paths;

import net.vbelev.web4.ui.WebUser;
import net.vbelev.web4.utils.*;
import net.vbelev.web4.xml.*;
import net.vbelev.web4.core.*;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
//import static com.mongodb.client.model.Filters.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;

public class GBMongoStorage implements GBEngine.Storage
{
	public final XMLiser xmliser;
	
	private final MongoClientURI connectionString;
	public final com.mongodb.MongoClient mongo;
	public final MongoDatabase mongoDB;
	private Date m_lastPing = null;
	
	public static class MongoTest
	{
		public int _id;
		public String fieldA;
		public double fieldB;
		public Date fieldD; 
	}
	
	public GBMongoStorage(String url)
	{
		//xmliser = new XMLiser("net.vbelev.web4.xml");
		xmliser = new XMLiser(
				GBGroupListXML.class, 
				GBBillXML.class, 
				GBProfileXML.class, 
				//GBMongoStorage.MongoTest.class,
				WebUserXML.class
				);

		//  com.mongodb.internal.operation.SyncOperations
		connectionString = new MongoClientURI("mongodb://web4:vbelevweb4@vbelev.net:27017/web4?authSource=admin");

		mongo = new com.mongodb.MongoClient(connectionString);
		mongoDB = mongo.getDatabase(connectionString.getDatabase());
		m_lastPing = new Date();
	}
	
	public void close()
	{
//		mongo.close();
	}
	
	public void finalize() throws Throwable
	{
		close();
	}
	
	public boolean ping(boolean force)
	{
		boolean res = false;
		
		if (!force && m_lastPing != null && m_lastPing.getTime() + 60000l < new Date().getTime())
		{
			return true;
		}
		try
		{
			res = mongo.listDatabaseNames().first() != null;
		}
		catch (IllegalArgumentException x)
		{
			throw x;
		}
		catch (Exception x2)
		{
			// try again, once.
			try
			{
				res = mongo.listDatabaseNames().first() != null;
			}
			catch (Exception x3)
			{
				res = false;
			}
		}
		m_lastPing = res? new Date() : null;
		
		return res;
	}
	
	public <T>T getOne(Class<T> elementClass, MongoIterable<Document> coll)
	{
		if (coll == null) return null;
		
		for(Document d : coll)
		{
			d.remove("_id");
			String json = d.toJson();
			return xmliser.fromXML(elementClass, json);
		}
		return null;
	}
	
	public <T>T getOneScalar(Class<T> elementClass, MongoIterable<Document> coll)
	{
		if (coll == null) return null;
		
		Document d1 = coll.first();
		if (d1 == null) return null;
		
		for (Object val : d1.values())
		{
			if (val == null)
			{
				return null;
			}
			else if (elementClass.isAssignableFrom(val.getClass()))
			{
				return (T)val;
			}
			else
			{
				throw new IllegalArgumentException("value of type " + val.getClass().getSimpleName() + " cannot be converted to " + elementClass.getSimpleName());
			}				
		}
		return null;
	}
	
	public <T>List<T> getList(Class<T> elementClass, MongoIterable<Document> coll)
	{
		ArrayList<T> res = new ArrayList<T>();
		if (coll == null) return res;
		for(Document d : coll)
		{
			String json = d.toJson();
			T elem = xmliser.fromXML(elementClass, json);
			res.add(elem);
				//xmliser.fromXML(elementClass, d.toJson())
			//);
		}
		return res;
	}
	
	
	public static Document mdbDocument(Object... attrs)
	{
		Document d = new Document();
		for (int i = 0; i < attrs.length;)
		{
			String attrName = (attrs[i] == null? "" : (attrs[i] instanceof String? (String)attrs[i] : attrs[i].toString()));
			Object attrValue = (++i >= attrs.length || attrs[i] == null) ? "" : attrs[i];
			i++;
			d.append(attrName, attrValue);
		}
		return d;
		
	}
	
	public static Object[] mdbArray(Object... elems)
	{
		//Object[] res = new Object[elems.length];
		return elems.clone();
	}
	
	public void testMongo()
	{
		/*
		MongoTest test = new MongoTest();
		test._id = 4;
		test.fieldA = "my fieldA2";
		test.fieldB = 1.17;
		test.fieldD = new Date();
		
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		
		xmliser.toXML(os, test);
		
		String xml = XMLiser.stringFromBytes(os.toByteArray());
		

		Document doc = Document.parse(xml);
		//MongoDatabase db = mongo.getDatabase("web4");
		MongoCollection<Document> coll = mongoDB.getCollection("gbtest");
		coll.insertOne(doc);
		*/
	}
	public GBGroupListXML getGroups()
	{
		ping(false);
		MongoCollection<Document> coll = mongoDB.getCollection(GBGroupListXML.STORAGE_NAME); //GBGroupListXML.STORAGE_NAME);
		//GBGroupListXML res = getOne(GBGroupListXML.class, coll.find());
				
		Document d3 = coll.find().first();
		if (d3 ==  null) return null;
		d3.remove("_id");
		GBGroupListXML res = xmliser.fromXML(GBGroupListXML.class, d3.toJson());
		d3 = null;
		
		
		//if (res == null)
		//{
		//	return new GBGroupListXML();
		//}
		return res;
	}
	
	public void saveGroups(GBGroupListXML xml)
	{
		ping(false);
		//Bson c;
		//mongoDB.runCommand(null)
		String xmlStr = xmliser.toXMLString(xml);
		Document d = Document.parse(xmlStr);
		d.put("_id", 1);
		GBGroupListXML xm2 = xmliser.fromXML(GBGroupListXML.class, d.toJson());
		MongoCollection<Document> coll = mongoDB.getCollection(GBGroupListXML.STORAGE_NAME);
		UpdateResult updRes = coll.replaceOne(Filters.eq("_id", 1), d, new ReplaceOptions().upsert(true)); // there is only one element, no filtering
		System.out.print(updRes.toString());
		d = null;
		/*
		 * should try this (from here: https://stackoverflow.com/questions/43711716/mongodb-upsert-exception-invalid-bson-field ) :
		 * query = Filters.eq("name", nameToSearch);
Document upsert = new Document();
Date now = new Date();

//only fields not mentioned in "$set" is needed here
Document toInsert = new Document()
        .append("age", newAge)
        .append("gender", genderString)
        .append("createAt", now);
//the fields to update here, whether on insert or on update.
Document toUpdate = new Document().append("name", nameToSearch)
        .append("updateAt", now);

//will: 
// - insert 5 fields if query returns no match
// - updates 2 fields if query returns match
upsert.append("$setOnInsert", toInsert)
        .append("$set", toUpdate);

UpdateResult result = collection.updateOne(query, toUpdate, 
    new UpdateOptions().upsert(true));
		 */
	}
	
	public List<GBBillXML> loadBills()
	{
		// here we read all bills into one list
		List<GBBillXML> res = getList(GBBillXML.class, mongoDB.getCollection(GBBillXML.STORAGE_NAME).find().projection(new Document("_id", 0)));
		
			return res;
	}
	

	public GBBillXML loadBill(int billID)
	{
		GBBillXML res = getOne(GBBillXML.class, 
				mongoDB.getCollection(GBBillXML.STORAGE_NAME)
				//.find(new Document("$gbBillXML.ID", billID))
				.find(Filters.eq("ID", billID))
				.projection(Projections.excludeId())
				);
		return res;
	}	
	
	public synchronized int getNewBillID(boolean withCreate) 
	{
		Document idDoc = mongoDB.getCollection(GBBillXML.STORAGE_NAME)
	 	.aggregate(
	 		Arrays.asList(
	 			Aggregates.group(
	 				null, // the grouping field, "$status" 
	 				Accumulators.max("maxID", "$ID")
	 			)
	 		)
		).first();
		Integer res = idDoc != null? (Integer)idDoc.get("maxID") : null;
		 if (res == null) 
			 res = 1;
		 else 
			 res++;
		 
		 if (withCreate) 
		 {
			 
			 mongoDB.getCollection(GBBillXML.STORAGE_NAME).replaceOne(
					 Filters.eq("ID", res), 
					 new Document("ID", res), 
					 new ReplaceOptions().upsert(true)
					 );	 
		 }
		return res;
	}

	public void saveBill(GBBillXML xml) 
	{
		if (xml == null) return;
		
		if (xml.ID == null)
		{
			xml.ID = this.getNewBillID(true);
		}
		
		String json = xmliser.toXMLString(xml);
		mongoDB.getCollection(GBBillXML.STORAGE_NAME).replaceOne(
				 Filters.eq("ID", xml.ID), 
				 Document.parse(json), 
				 new ReplaceOptions().upsert(true)
				 );	 
	}
	
	//==== GBProfile storage ====
	public GBProfileXML loadProfile(int profileID)
	{
		//GBProfileXML res = getOne(GBProfileXML.class, mongoDB.getCollection(GBProfileXML.STORAGE_NAME).find(new Document("ID", profileID)));
		GBProfileXML res = getOne(GBProfileXML.class, 
				mongoDB.getCollection(GBProfileXML.STORAGE_NAME)
				//.find(new Document("$gbBillXML.ID", billID))
				.find(Filters.eq("ID", profileID))
				.projection(Projections.excludeId())
				);
		return res;
	}

	public synchronized int getNewProfileID(boolean withCreate)
	{
		Document idDoc = mongoDB.getCollection(GBProfileXML.STORAGE_NAME)
			 	.aggregate(
			 		Arrays.asList(
			 			Aggregates.group(
			 				null, // the grouping field, "$status" 
			 				Accumulators.max("ID", "$ID")
			 			)
			 		)
				).first();
				Integer res = idDoc != null? (Integer)idDoc.get("ID") : null;
				 if (res == null) 
					 res = 1;
				 else 
					 res++;
				 
				 if (withCreate) 
				 {
					 
					 mongoDB.getCollection(GBProfileXML.STORAGE_NAME).replaceOne(
							 Filters.eq("ID", res), 
							 new Document("ID", res), 
							 new ReplaceOptions().upsert(true)
							 );	 
				 }
				return res;

	}
	
	public void saveProfile(GBProfileXML xml) {
		if (xml == null) return;
		
		if (xml.ID == null)
		{
			xml.ID = this.getNewProfileID(true);
		}
		
		String json = xmliser.toXMLString(xml);
		mongoDB.getCollection(GBProfileXML.STORAGE_NAME).replaceOne(
				 Filters.eq("ID", xml.ID), 
				 Document.parse(json), 
				 new ReplaceOptions().upsert(true)
				 );	 

	}
	
	//==== WebUser storage ====
	public WebUserXML loadWebUser(int webUserID)
	{
		//WebUserXML res = getOne(WebUserXML.class, mongoDB.getCollection(WebUserXML.STORAGE_NAME).find(new Document("ID", webUserID)));
		WebUserXML res = getOne(WebUserXML.class, 
				mongoDB.getCollection(WebUserXML.STORAGE_NAME)
				//.find(new Document("$gbBillXML.ID", billID))
				.find(Filters.eq("ID", webUserID))
				.projection(Projections.excludeId())
				);
		return res;
	}
	
	public static final java.util.regex.Pattern xmlIdPattern = java.util.regex.Pattern.compile("^(\\d+).xml$");
	
	public int loadWebUserIndex(Hashtable<String, Integer> index)
	{
		index.clear();
		
		String command = "{\r\n" + 
				"find: \"" + WebUserXML.STORAGE_NAME + "\",\r\n" + 
				"filter: {},\r\n" +
				"batchSize: 999999,\r\n" +
				"projection: { _id: 0, \"ID\" : 1, \"login\": 1}\r\n" + 
				"}";
		
		//mongoDB.getCollection("web_users").find();
		Document d = mongoDB.runCommand(Document.parse(command));
		for (Document item : ((Document)d.get("cursor")).getList("firstBatch", Document.class))
		{
			//Document webUser = (Document)item.get("web_user");
			Document webUser = item;
			if (webUser != null) 
			{
			int id = webUser.getInteger("ID", 0);
			String login = webUser.get("login", String.class);
			index.put(Utils.NVL(login, "#" + id).trim().toLowerCase(), id);
			}
			
		}
		return index.size();
	}
	
	public int getNewWebUserID(boolean withCreate) 
	{
		Integer res = getOneScalar(Integer.class, 
				mongoDB.getCollection(WebUserXML.STORAGE_NAME)
				 	.aggregate(
				 		Arrays.asList(
				 			Aggregates.group(
				 				null, // the grouping field, "$status" 
				 				Accumulators.max("ID", "$ID")
				 			)
				 		)
					)
				 );
		 if (res == null) res = 1;
		 else res = res + 1;
		 
		 if (withCreate) 
		 {
 
			 mongoDB.getCollection(WebUserXML.STORAGE_NAME).replaceOne(
					 Filters.eq("ID", res), 
					 new Document("ID", res), 
					 new ReplaceOptions().upsert(true)
					 );	 
			 
		 }
		return res;
	}
	
	public void saveWebUser(WebUserXML xml) 
	{
		if (xml == null) return;
		
		if (xml.ID == null)
		{
			xml.ID = this.getNewProfileID(true);
		}
		
		String json = xmliser.toXMLString(xml);
		mongoDB.getCollection(WebUserXML.STORAGE_NAME).replaceOne(
				 Filters.eq("ID", xml.ID), 
				 //Filters.eq("ID", xml.ID), 
				 Document.parse(json), 
				 new ReplaceOptions().upsert(true)
				 );
	}
	

}
