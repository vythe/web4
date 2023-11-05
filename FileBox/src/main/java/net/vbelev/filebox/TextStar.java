package net.vbelev.filebox;
import java.io.*;
import java.util.*;

import org.htmlcleaner.*;

/**
 * The text piece descriptor: the source info (that's the file name), the full text
 * and the word vector.
 */
public class TextStar
{
	public String source;
	public String text;
	public int domainIndex;
	public WordMetric.Vector vector;
	
	public void fromStream(WordMetric metric, InputStream s) throws IOException
	{
		InputStreamReader reader = new InputStreamReader(s);
		BufferedReader br = new BufferedReader(reader);
		String fullText = "";
		String line;
		while ((line = br.readLine()) != null)
		{
			fullText += line;
		}
		HtmlCleaner cleaner = new HtmlCleaner();
		
		TagNode doc = cleaner.clean(fullText);
		StringBuilder builder = new StringBuilder();
		getNodeText(builder, doc); //.getText().toString();
		text = builder.toString();
		//text = doc.getText().toString();
		vector = metric.parse(text);
		br.close();
	}
	
	public void getNodeText(StringBuilder builder, TagNode node)
	{
		if (!node.hasChildren())
		{
			CharSequence txt = node.getText();
			if (txt.length() > 0)
			{
				builder.append(txt);
			}
			return;
		}
		for (BaseToken child : node.getAllChildren())
		{
			int builderLen = builder.length();
			if (child instanceof TagNode childNode)
			{
				if (!"style".equals(childNode.getName().toLowerCase()) && !"script".equals(childNode.getName().toLowerCase()))
				{
					getNodeText(builder, childNode);
				}			
			}
			else if (child instanceof ContentNode contentNode)
			{
				CharSequence txt = contentNode.getContent();
				if (txt.length() > 0)
				{
					builder.append(txt);
				}			
			}
			if (builder.length() > builderLen && builder.charAt(builder.length() - 1) != ' ')
			{
				builder.append(" ");							
			}
		}
	}
}
