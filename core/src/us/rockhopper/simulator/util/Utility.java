package us.rockhopper.simulator.util;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;

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

	public static Color colorFromHSV(float hue, float saturation, float value, float alpha) {
		int h = (int) (hue * 6);
		float f = hue * 6 - h;
		float p = value * (1 - saturation);
		float q = value * (1 - f * saturation);
		float t = value * (1 - (1 - f) * saturation);

		switch (h) {
		case 0:
			return new Color(value, t, p, alpha);
		case 1:
			return new Color(q, value, p, alpha);
		case 2:
			return new Color(p, value, t, alpha);
		case 3:
			return new Color(p, q, value, alpha);
		case 4:
			return new Color(t, p, value, alpha);
		case 5:
			return new Color(value, p, q, alpha);
		default:
			throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + hue + ", "
					+ saturation + ", " + value);
		}
	}
}