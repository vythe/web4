package net.vbelev.utils.test;

//import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import net.vbelev.utils.*;

class RijndaelTest
{

	@Test
	void test()
	{
		String val1 = "testing"; //"My test string";
		RijndaelCrypt c = new RijndaelCrypt("The quick brown fox jumps", "over the lazy dog.");
		String encrypted = c.encrypt(val1.getBytes());
		String decrypted = c.decrypt(encrypted);
		System.out.println("val1=" + val1 + ", encrypted=" + encrypted);
		Assertions.assertEquals(val1, decrypted);
	}

}
