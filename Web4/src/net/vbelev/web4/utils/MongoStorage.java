package net.vbelev.web4.utils;

import java.util.*;

import org.bson.*;
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



public class MongoStorage
{
	
	public static BsonDocument mdbDocument(Object... attrs)
	{
		BsonDocument d = new BsonDocument();
		for (int i = 0; i < attrs.length;)
		{
			String attrName = (attrs[i] == null? "" : (attrs[i] instanceof String? (String)attrs[i] : attrs[i].toString()));
			Object attrValue = (++i >= attrs.length || attrs[i] == null) ? "" : attrs[i];
			i++;
			d.append(attrName, mdbValue(attrValue));
		}
		//BsonDocument bd;
		//bd.
		
		return d;
		
	}
	
	public static BsonValue mdbValue(Object val)
	{
		if (val == null) return new BsonNull();
		if (val instanceof BsonValue) return (BsonValue)val;
		if (val instanceof String) return new BsonString((String)val);
		if (val instanceof Integer) return new BsonInt32((int)val);
		if (val instanceof Long) return new BsonInt64((long)val);
		if (val instanceof Double) return new BsonDouble((double)val);
		if (val instanceof Date) return new BsonDateTime(((Date)val).getTime());
				
		return new BsonString(val.toString());
	}
	
	public static BsonArray mdbArray(Object... elems)
	{
		BsonArray arr = new BsonArray();// {"a", "b"}
		if (elems == null || elems.length == 0) return arr;
		
		for (Object elem : elems)
		{
			BsonValue v = mdbValue(elem);
			arr.add(v);
		}
		return arr;
	}

}
