package us.rockhopper.simulator.util;

import java.util.ArrayList;
import java.util.List;

public class Utility {
	// The load is not evenly distributed so that the lists can be more easily
	// combined back together in the same order.
	public static <E> List<List<E>> splitList(List<E> list, int parts) {
		List<List<E>> grouped = new ArrayList<List<E>>();
		for (int i = 0; i < parts; ++i) {
			grouped.add(new ArrayList<E>());
		}

		int sectorSize = list.size() / parts;
		int i = 0;
		int group = 0;
		List<E> current = new ArrayList<E>();
		for (E elem : list) {
			current.add(elem);
			++i;
			if (i >= sectorSize && group < parts) {
				grouped.get(group).addAll(current);
				current.clear();
				i = 0;
				group++;
			}
		}

		// Distribute the remainder
		for (int j = 0; j < current.size(); ++j) {
			grouped.get(grouped.size() - 1).add(current.get(j));
		}

		return grouped;
	}

	public static boolean floatEquals(float a, float b, float epsilon) {
		return (Math.abs(a - b) < epsilon);
	}

	public static <E> List<E> rotateToFirst(E o, List<E> objs) {
		List<E> temp = new ArrayList<E>();
		for (int i = 0; i < objs.size(); ++i) {
			if (objs.get(i).equals(o)) {
				for (int k = i; k < objs.size(); ++k) {
					temp.add(objs.get(k));
				}
				for (int k = 0; k < i; ++k) {
					temp.add(objs.get(k));
				}
				break;
			}
		}
		return temp;
	}
}