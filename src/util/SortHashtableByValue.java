package util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
/**
 * This class sort hashtable by value
 * @author Jing Zhang
 */
public class SortHashtableByValue {
	/**
	 * @param table
	 * @return Map.Entry[]
	 */
	public static Map.Entry[] sort( Hashtable table)
	{
		Set set = table.entrySet();
		Map.Entry[]  entries = (Map.Entry[]) set.toArray(new Map.Entry[set.size()]);
		Arrays.sort(entries, new Comparator() {
		    public int compare(Object o1, Object o2) {
			Object v1 = ((Map.Entry) o1).getValue();
			Object v2 = ((Map.Entry) o2).getValue();
			return ((Comparable) v2).compareTo(v1);
		          }
		});
		return entries;
	}

}
