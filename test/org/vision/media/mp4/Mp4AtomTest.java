/***
 * The content of this file or document is CONFIDENTIAL and PROPRIETARY
 * to ChengZhen(anyou@msn.com). It is subject to the terms of a
 * License Agreement between Licensee and ChengZhen(anyou@msn.com).
 * restricting among other things, the use, reproduction, distribution
 * and transfer. Each of the embodiments, including this information and
 * any derivative work shall retain this copyright notice.
 *
 * Copyright (c) 2005-2014 ChengZhen(anyou@msn.com) All Rights Reserved.
 *
 */
package org.vision.media.mp4;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.vision.media.mp4.Mp4Atom.getAtomId;
import static org.vision.media.mp4.Mp4PropertyType.PT_DESCRIPTOR;
import static org.vision.media.mp4.Mp4PropertyType.PT_FLOAT;
import static org.vision.media.mp4.Mp4PropertyType.PT_INT;
import static org.vision.media.mp4.Mp4PropertyType.PT_STRING;
import static org.vision.media.mp4.Mp4PropertyType.PT_TABLE;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

public class Mp4AtomTest {

	@Test
	public void testAddChildAtom() {
		Mp4Atom atom = Mp4Factory.getInstanae().newAtom("mvhd");

		try {
			atom.addChildAtom((String) null);
			fail("expect IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}

		try {
			atom.addChildAtom(" ");
			fail("expect IllegalArgumentException");
		} catch (IllegalArgumentException e) {
		}

		// ATOM
		int count = atom.getChildAtoms().size();
		Mp4Atom child = atom.addChildAtom("free");
		assertNotNull(child);
		assertEquals(Mp4Atom.getAtomId("free"), child.getType());
		assertEquals(count + 1, atom.getChildAtoms().size());

		// ATOM
		child = atom.addChildAtom("udat.test");
		assertNotNull(child);
		assertEquals(Mp4Atom.getAtomId("test"), child.getType());
		assertEquals(count + 2, atom.getChildAtoms().size());

		Mp4Atom parent = child.getParentAtom();
		assertNotNull(parent);
		assertEquals(Mp4Atom.getAtomId("udat"), parent.getType());
		assertEquals(1, parent.getChildAtoms().size());

		Mp4Atom udat = atom.getChildAtoms()
				.get(atom.getChildAtoms().size() - 1);
		assertEquals(Mp4Atom.getAtomId("udat"), udat.getType());
		assertEquals(1, udat.getChildAtoms().size());

		// 
		child = atom.addChildAtom(0, "mdat");
		Mp4Atom mdat = atom.getChildAtoms().get(0);
		assertEquals(Mp4Atom.getAtomId("mdat"), mdat.getType());

		parent = child.getParentAtom();
		assertNotNull(parent);
		assertEquals(Mp4Atom.getAtomId("mvhd"), parent.getType());

	}

	@Test
	public void testAddProperty() {
		Mp4Atom atom = Mp4Factory.getInstanae().newAtom("udat");

		assertEquals(null, atom.addProperty(PT_TABLE, 0, "table"));
		assertEquals(null, atom.addProperty(PT_DESCRIPTOR, 0, "table"));

		Mp4Property property = atom.addProperty(PT_INT, 4, "int");
		assertNotNull(property);
		assertEquals(4, property.getSize());
		assertEquals("int", property.getName());

		property = atom.getProperty(0);
		assertNotNull(property);
		assertEquals(4, property.getSize());
		assertEquals("int", property.getName());

		property = atom.addProperty(PT_FLOAT, 2, "float");
		property = atom.getProperty(1);
		assertNotNull(property);
		assertEquals(2, property.getSize());
		assertEquals("float", property.getName());
	}

	/**
	 * Atom
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAtom() throws IOException {
		Mp4Atom atom = Mp4Factory.getInstanae().newAtom("mvhd");

		List<Mp4Property> list = atom.getProperties();
		assertEquals(11, list.size());

		atom.setProperty("timescale", 1000);
		assertEquals(1000, atom.getPropertyInt("timescale"));

		atom.setProperty("rate", 5.5f);
		assertEquals(5.5f, atom.getPropertyFloat("rate"), 0.1f);

		atom.setProperty("next_track_ID", 2);
		assertEquals(2, atom.getPropertyInt("next_track_ID"));

		atom.updateSize();
		assertEquals(108, atom.getSize());
	}

	@Test
	public void testFindAtom() {
		Mp4Atom atom = Mp4Factory.getInstanae().newAtom("mvhd");
		atom.addChildAtom("free");
		atom.addChildAtom("udat.test");
		atom.addChildAtom(0, "mdat");

		assertEquals(null, atom.findAtom(null));
		assertEquals(null, atom.findAtom(""));
		assertEquals(null, atom.findAtom("fre"));
		assertEquals(null, atom.findAtom("test"));
		assertEquals(null, atom.findAtom("udat.free"));

		assertEquals(getAtomId("free"), atom.findAtom("free").getType());
		assertEquals(getAtomId("test"), atom.findAtom("udat.test").getType());
		assertEquals(getAtomId("mdat"), atom.findAtom("mdat").getType());
	}

	@Test
	public void testGetAtomId() {
		assertEquals(-1, Mp4Atom.getAtomId(null));
		assertEquals(-1, Mp4Atom.getAtomId(""));
		assertEquals(-1, Mp4Atom.getAtomId("abc"));
		assertEquals(1953653099, Mp4Atom.getAtomId("trak"));
		assertEquals(1953653099, Mp4Atom.getAtomId("trakc"));
	}

	@Test
	public void testGetAtomType() {
		System.out.println(Mp4Atom.getAtomType(-1));
		System.out.println(Mp4Atom.getAtomType(0));
		Mp4Atom.getAtomType(999999);
		Mp4Atom.getAtomType(-999999);

		int type = Mp4Atom.getAtomId("trak");
		assertEquals("trak", Mp4Atom.getAtomType(type));

		type = Mp4Atom.getAtomId("avc ");
		assertEquals("avc ", Mp4Atom.getAtomType(type));

		String text = Mp4Atom.getAtomType(type);
		assertEquals(type, Mp4Atom.getAtomId(text));
	}

	@Test
	public void testGetChildAtom() {
		Mp4Atom atom = Mp4Factory.getInstanae().newAtom("mvhd");
		atom.addChildAtom("free");
		atom.addChildAtom("udat.test");
		atom.addChildAtom(0, "mdat");

		assertEquals(null, atom.getChildAtom(null));
		assertEquals(null, atom.getChildAtom(""));
		assertEquals(null, atom.getChildAtom("fre"));
		assertEquals(null, atom.getChildAtom("test"));

		assertEquals(getAtomId("free"), atom.getChildAtom("free").getType());
		assertEquals(getAtomId("udat"), atom.getChildAtom("udat.test")
				.getType());
		assertEquals(getAtomId("mdat"), atom.getChildAtom("mdat").getType());

		assertEquals(null, atom.getChildAtom(-1));
		assertEquals(null, atom.getChildAtom(3));
		assertEquals(null, atom.getChildAtom(9999));

		assertEquals(getAtomId("mdat"), atom.getChildAtom(0).getType());
		assertEquals(getAtomId("free"), atom.getChildAtom(1).getType());
		assertEquals(getAtomId("udat"), atom.getChildAtom(2).getType());
	}

	@Test
	public void testGetSetProperty() {
		Mp4Atom atom = Mp4Factory.getInstanae().newAtom("udat");
		atom.addProperty(PT_INT, 4, "int");
		atom.addProperty(PT_FLOAT, 2, "float");
		atom.addProperty(PT_STRING, 8, "string");

		atom.setProperty("size", 199);
		atom.setProperty("size", 199.0f);
		atom.setProperty("size", "199");

		atom.setProperty("int", 199);
		atom.setProperty("float", 129.2f);
		atom.setProperty("string", "abcde");

		assertEquals(199, atom.getPropertyInt("int"));
		assertEquals(129.2f, atom.getPropertyFloat("float"), 0.1f);
		assertEquals("abcde\0\0\0", atom.getPropertyString("string"));

		atom.setProperty("string", "abcd1234==");
		assertEquals("abcd1234", atom.getPropertyString("string"));
	}

	@Test
	public void testUpdateSize() {
		Mp4Atom atom = Mp4Factory.getInstanae().newAtom("moov");

		atom.updateSize();
		// Mp4Test.printAtom(atom);
	}
}
