import java.util.*;
import javax.management.relation.*;
import javax.management.*;

public class Sort15 {

  public static void main(String[] a){
    // add our roles to the RoleList
    RoleList libraryList = new RoleList();

    populateRoleList(libraryList);


    for( Object each : libraryList){
      final Role eachRole = (Role) each;

      System.out.println(eachRole);
    }
  }

  public static void populateRoleList(RoleList libraryList){
    // building the owner Role
	// ObjectName earExpression=new ObjectName
    List<ObjectName> ownerList = new ArrayList<ObjectName>();
    final Role ownerRole = new Role("owner", ownerList);

    // building the book role
    List<ObjectName> bookList = new ArrayList<ObjectName>();
    Role bookRole = new Role("books", bookList);

    libraryList.add(ownerRole);
    libraryList.add(bookRole);
  }
}