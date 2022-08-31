# struct-of-array
Two data structure with the API of an array of structs (AoS) and the internals of a struct of arrays (SoA)

The API provides two data structure, a `StructOfArrayList` that have an API similar to an `ArrayList`
of records but stores the components of the record in different arrays and a `StructOfArrayMap`
that works like `HashMap` of integers (a sparse array) using the same destructuring.

```java
  record Person(int age, String name) {}
  
  var soaList = StructOfArrayList.of(MethodHandles.lookup(), Person.class);
  soaList.add(new Person(36, "Ana"));
  soaList.add(new Person(18, "Bob"));
  System.out.println(soaList.get(0));  // Person[36, Ana]
```

Internally, the list stores an array of ints for the ages and an array of objects (the reference types are erased)
for the names.

And a similar example using `StructOfArrayMap`
```java
  var soaMap = StructOfArrayMap.of(MethodHandles.lookup(), Person.class);
  soaMap.put(10, new Person(36, "Ana"));
  soaMap.put(6, new Person(18, "Bob"));
  System.out.println(soaList.get(6));  // Person[18, Bob]
```


### How to build ?
Just use Maven with Java 17+
```bash
  mvn package
```



