package org.vision.media.mp4;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;
import org.vision.media.avc.Mp4VideoUtils;
import org.vision.media.avc.Mp4VideoUtils.AvcSeqParams;
import org.vision.utils.Base64;



public class Mp4Test {

	/** 打印指定的 Atom */
	public static void printAtom(Mp4Atom atom) {
		int depth = atom.getDepth();
		String indent = "";
		for (int i = 0; i < depth; i++) {
			indent += "  ";
		}
		System.out.printf(indent + "<%s size=\"%d\">\r\n", atom.getTypeString(), atom.getSize());
		for (Mp4Property property : atom.getProperties()) {
			if (property.getType() == Mp4PropertyType.PT_TABLE) {
				System.out.printf(indent + "  <property name=\"%s\">\r\n", property.getName());
				print(indent, (Mp4TableProperty)property);
				System.out.printf(indent + "  </property>\r\n");
			} else {
				System.out.printf(indent + "  <property name=\"%s\" size=\"%d\">%s</property>\r\n",
						property.getName(), property.getSize(), property.getValueString());
			}
		}
		for (Mp4Atom child: atom.getChildAtoms()) {
			printAtom(child);
		}
		System.out.println(indent + "</" + atom.getTypeString() + ">");
	}

	public static void print(String indent, Mp4TableProperty table) {
		System.out.print(indent + "    <l>");
		for (int i = 0; i < table.getFieldCount(); i++) {
			System.out.print("<h>" + table.getFieldName(i) + "</h>");
		}
		System.out.print("</l>\r\n");

		int i = 0;
		for (long[] line : table.getRows()) {
			if (i > 20) {
				System.out.print(indent + "    <l><more/><l/>\r\n");
				break;
			}
			System.out.print(indent + "    <l>");
			for (long element : line) {
				System.out.print("<c>" + element + "</c>");
			}
			System.out.print("</l>\r\n");
			i++;
		}

	}

	@SuppressWarnings("unused")
	protected void print(Mp4SizeTableProperty table) {
		System.out.println("\t Size Table |");
		for (byte[] line : table.getRows()) {
			//System.out.println(line.length + ": " + StringUtils.encodeHex(line) + "|");
		}
	}


	@SuppressWarnings("unchecked")
	protected <T> T getFieldValue(Object object, String name) {
		Class<?> clazz = object.getClass();
		Field field = null;
		try {
			field = clazz.getDeclaredField(name);
			field.setAccessible(true);
			return (T) field.get(object);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (field != null) {
				field.setAccessible(false);
			}
		}
		return null;
	}

	protected <T> Object testMethod(T obj, String name, Object...args) throws Throwable {
		Class<?> clazz = obj.getClass();
		for (Method method : clazz.getMethods()) {
			if (name.equals(method.getName())) {
				try {
					method.invoke(obj, args);
				} catch (IllegalArgumentException e) {
					throw e;
				} catch (IllegalAccessException e) {
					throw e;
				} catch (InvocationTargetException e) {
					throw e.getTargetException();
				}
			}
		}
		return null;
	}
	
	@Test public void testVideo() {
		String set = "Z0LgDdqFglE=";
		byte[] data = Base64.decode(set);
		System.out.println(data.length);
		//System.out.println(StringUtils.encodeHex(data));
		
		AvcSeqParams params = Mp4VideoUtils.readSeqInfo(data);
		System.out.println("Profile: " + params.profile);
		System.out.println("Width: " + params.pic_width);
		System.out.println("Height: " + params.pic_height);
	}
}
