import java.util.Iterator;
import java.util.TreeSet;

public class Sort13 {
  public static void main(String[] args) {

    // create ascending iterator
    Iterator<String> iterator = ascending("1", "13", "17", "2");

    // displaying the Tree set data
    System.out.println("Tree set data in ascending order: ");
    while (iterator.hasNext()){
      System.out.println(iterator.next() + " ");
    }
  }

  public static Iterator<String> ascending(String... args){
    // creating a TreeSet
    TreeSet<String> treeadd = new TreeSet<String>();

    // adding strings to set
    for(String each : args){
      treeadd.add(each);
    }

    return treeadd.iterator();
  }
}