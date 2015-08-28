package us.rockhopper.simulator.tests;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import us.rockhopper.simulator.util.Utility;

public class UtilityTest {

	int[] iArr = { 1, 2, 3 };
	List<Integer> intList = new ArrayList<Integer>();

	String[] sArr = { "1", "2", "3" };
	List<String> sList = new ArrayList<String>();

	private void initialize() {
		for (int i : iArr) {
			intList.add(i);
		}
		for (String i : sArr) {
			sList.add(i);
		}
	}

	@Test
	public void testStringSplitPrimitives() {
		this.initialize();
		List<List<Integer>> ll = Utility.splitList(intList, 3);
		assertEquals((int) ll.get(0).get(0), 1);
		assertEquals((int) ll.get(1).get(0), 2);
		assertEquals((int) ll.get(2).get(0), 3);
	}

	@Test
	public void testStringSplitObjects() {
		this.initialize();
		List<List<String>> ll = Utility.splitList(sList, 3);
		assertEquals(ll.get(0).get(0), sArr[0]);
		assertEquals(ll.get(1).get(0), sArr[1]);
		assertEquals(ll.get(2).get(0), sArr[2]);
	}

	@Test
	public void testFloatEpsilon() {
		assertTrue(Utility.floatEquals(0.001f, 0.001f, 0.000001f));
	}
	
	@Test
	public void testFloatEpsilon1() {
		assertFalse(Utility.floatEquals(0.001f, 0.002f, 0.000001f));
	}
	
	@Test
	public void testFloatEpsilon2() {
		assertTrue(Utility.floatEquals(0.001f, 0.002f, 0.002f));
	}
	
    @Test
    public void testRotate() {
    	List<Integer> list = new ArrayList<Integer>();
    	list.add(1);
    	list.add(2);
    	list.add(3);
    	list = Utility.rotateToFirst((Integer) 2, list);
    	assertEquals(list.get(0), (Integer) 2);
    	assertEquals(list.get(1), (Integer) 3);
    	assertEquals(list.get(2), (Integer) 1);
    }
}