import java.util.*;

public class Sort12 {
  public static void main(String args[]) {
    List<String> set = new ArrayList<String>();

    // populate the list
    set.add("dragon");
    set.add("zebra");
    set.add("tigers");
    set.add("lions");

    final List<String> list = sort(set);

    for(String s : list){
      System.out.println(s + " ");
    }
  }

  public static List<String> sort(List<String> list){
    final List<String> copy = new ArrayList<String>(list);

    Collections.sort(copy.subList(0, copy.size()));

    return copy;
  }
}