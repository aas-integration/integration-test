import java.util.*;

public class Sort11 {
  public static void main(String args[]) {
    // create linked list object
    LinkedHashSet<Integer> set = new LinkedHashSet<Integer>();

    // populate the list
    set.add(-28);
    set.add(20);
    set.add(-12);
    set.add(8);

    final List<Integer> list = sortSet(set);

    System.out.println("LinkedHashSet sorted in ReverseOrder: ");
    for(int i : list){
      System.out.println(i + " ");
    }
  }

  public static List<Integer> sortSet(Set<Integer> set){
    final List<Integer> list = new ArrayList<Integer>(set);

    // sort the list in reverse order
    Collections.reverse(list);

    return list;
  }
}