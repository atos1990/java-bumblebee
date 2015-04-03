# Introduction #

Java Enterprise and/or web applications are cluttered with anti-patterns. Some of them, such as service locators, have - luckily - been cured, but some of them still plague developers on a far to daily basis. One of the more painful anti-patterns is DTO assembly; since Java EE / Spring involves transactional boundaries, we often want to ship data objects rather than domain objects back to controllers/web. Bumblebee can be used to simplify this a bit.

# Getting started #

Well, Bumblebee may help cure boring DTO conversions, but it's still a freakin' pain to get it up and running. Since it's not considered stable yet, there's no deployed artifact in a Maven repository, so you'll need to check it out from Subversion and build it. To complicate it further, the project depends on the Java Declarative Programming API - yet another project that is not considered stable and thus not available in a maven repository near you. So, this is what you'll need to do:

  1. Check out JDPA from https://jdpa.svn.sourceforge.net/svnroot/jdpa.
  1. Check out Bumblebee from http://java-bumblebee.googlecode.com/svn/trunk.
  1. Build JDPA using "mvn -Dmaven.test.skip install".
  1. Build Bumblebee using "mvn -Dmaven.test.skip install".

All necessary artifacts are now deployed locally, so it's now ready to use!

# Usage #

Bumblebee uses annotations and interfaces to define data transfer objects. Consider the following JPA entities:

```
@Entity
public class User {

   public String getUsername() { ... }

   @OneToOne(...)
   public ContactInformation getContactInformation() { ... }

}

@Entity
public class ContactInformation {

   public String getHomePhone() { ... }

}
```

An example of data objects we can use to transfer this accross a transactional boundary is:

```
@DataObject
public interface UserData {

   @Value
   public String getUsername();

   @Value
   public ContactInformation getContactInformation();

}

@DataObject
public interface ContactInformation {

   @Value
   public String getHomePhone();

}
```

You can also merge the information into the root DTO if you want to:
```
@DataObject
public interface UserData {
   
   @Value
   public String getUsername();

   @Value("contactInformation.homePhone")
   public String getHomePhone();
}
```

You then need to instruct Bumblebee to do the actual conversion in you business layer:

```
import static com.googlecode.bumblebee.dto.Bumblebee.assemble;

@Stateless // (or @Service or whatever)
public class MyServiceBean implements MyService {
   public UserData getUser(String username) {
      User user = findUserByUsername(username);

      return assemble(UserData.class).from(user);
   }
}
```

# Multiple nested values #

It's possible to extract nested data from a source object even though no distinct instance exists. Consider a user with multiple phone numbers:

```
public class User {

   public List<ContactInfo> getContactInfo() { ... }

}

public class ContactInfo {

   public String getPhoneNumber();

}
```

Given the entities above, we can define a DTO that extracts the phone numbers only, rather than dealing with cascaded DTOs:

```
@DataObject
public interface UserData {

   @Value("contactInfo.phoneNumber")
   public List<String> getPhoneNumbers();

}
```

The resulting list will be the union of all reachable leaves.