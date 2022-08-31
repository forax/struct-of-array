package com.github.forax.soa;

import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.stream.Stream;

import static java.lang.invoke.MethodType.methodType;

final class RT {
  private RT() {}

  private static MethodHandle constructor(Class<?> specializedClass, MethodType methodType, Class<?> baseClass) {
    try {
      var constructor = LOOKUP.findConstructor(specializedClass, methodType);
      return constructor.asType(constructor.type().changeReturnType(baseClass));
    } catch (IllegalAccessException | NoSuchMethodException e) {
      throw (LinkageError) new LinkageError().initCause(e);
    }
  }

  private record Species(Class<?> specializedClass, Lookup recordLookup, MethodHandle defaultConstructor) {
    public Class<?> recordType() {
      return recordLookup.lookupClass();
    }
  }


  private static final ThreadLocal<Species> SPECIES_LOCAL = new ThreadLocal<>();
  private static final ClassValue<Species> TEMPLATE = new ClassValue<>() {
    @Override
    protected Species computeValue(Class<?> type) {
      var species = SPECIES_LOCAL.get();
      if (species == null) {
        throw new LinkageError(type.getName() + " is not a specialized class");
      }
      return species;
    }
  };

  private static void injectSpecies(Class<?> specializedClass, Species species) {
    Species injectedSpecies;
    SPECIES_LOCAL.set(species);
    try {
      injectedSpecies = TEMPLATE.get(specializedClass);
    } finally {
      SPECIES_LOCAL.set(null);
    }
    if (species != injectedSpecies) {
      throw new AssertionError();
    }
  }

  static Species species(Class<?> specializedClass) {
    return TEMPLATE.get(specializedClass);
  }

  private static final Lookup LOOKUP = MethodHandles.lookup();

  private static final ThreadLocal<Lookup> LOOKUP_LOCAL = new ThreadLocal<>();
  private static final ClassValue<Species> SPECIES_LIST =
      classValue(StructOfArrayList$Template.class, methodType(void.class, int.class, boolean.class), StructOfArrayList.class);
  private static final ClassValue<Species> SPECIES_MAP =
      classValue(StructOfArrayMap$Template.class, methodType(void.class, int.class), StructOfArrayMap.class);

  private static ClassValue<Species> classValue(Class<?> template, MethodType constructorType, Class<?> baseClass) {
    return new ClassValue<>() {
      @Override
      protected Species computeValue(Class<?> type) {
        var generator = TemplateGenerator.specialized(template, type);
        var bytecode = generator.generate();
        Class<?> specializedClass;
        try {
          specializedClass = LOOKUP.defineClass(bytecode);
        } catch (IllegalAccessException e) {
          throw (LinkageError) new LinkageError().initCause(e);
        }
        var defaultConstructor = constructor(specializedClass, constructorType, baseClass);
        var recordLookup = LOOKUP_LOCAL.get();
        if (recordLookup == null) {
          throw new AssertionError("record lookup not specified");
        }
        var species = new Species(specializedClass, recordLookup, defaultConstructor);
        injectSpecies(specializedClass, species);
        return species;
      }
    };
  }

  static MethodHandle defaultListConstructor(Lookup recordLookup) {
    LOOKUP_LOCAL.set(recordLookup);
    try {
      return SPECIES_LIST.get(recordLookup.lookupClass()).defaultConstructor();
    } finally {
      LOOKUP_LOCAL.set(recordLookup);
    }
  }

  static MethodHandle defaultMapConstructor(Lookup recordLookup) {
    LOOKUP_LOCAL.set(recordLookup);
    try {
      return SPECIES_MAP.get(recordLookup.lookupClass()).defaultConstructor();
    } finally {
      LOOKUP_LOCAL.set(recordLookup);
    }
  }


  private static final MethodHandle INSTANCEOF, CHECKCAST;
  static {
    var lookup = MethodHandles.publicLookup();
    try {
      INSTANCEOF = lookup.findVirtual(Class.class, "isInstance", methodType(boolean.class, Object.class));
      CHECKCAST = lookup.findVirtual(Class.class, "cast", methodType(Object.class, Object.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new AssertionError(e);
    }
  }

  static CallSite bsm(Lookup lookup, String name, MethodType methodType) {
    var specializedClass = lookup.lookupClass();
    var species = species(specializedClass);
    var recordType = species.recordType();
    return switch (name) {
      case "newCanonicalList" -> {
        var speciesList = SPECIES_LIST.get(recordType);
        var canonicalConstructor = constructor(speciesList.specializedClass(), methodType.changeReturnType(void.class), StructOfArrayList.class);
        yield new ConstantCallSite(canonicalConstructor.asType(methodType));
      }
      case "new" -> {
        MethodHandle recordConstructor;
        try {
          recordConstructor = species.recordLookup()
              .findConstructor(recordType, methodType(void.class, Arrays.stream(recordType.getRecordComponents()).<Class<?>>map(RecordComponent::getType).toList()));
        } catch (NoSuchMethodException | IllegalAccessException e) {
          throw new AssertionError(e);
        }
        yield new ConstantCallSite(recordConstructor.asType(methodType));
      }
      case "instanceof" -> new ConstantCallSite(INSTANCEOF.bindTo(recordType));
      case "checkcast" -> new ConstantCallSite(CHECKCAST.bindTo(recordType));
      default -> throw new LinkageError("invalid name " + name);
    };
  }

  static CallSite bsm_record_access(Lookup lookup, String name, MethodType methodType) {
    var specializedClass = lookup.lookupClass();
    var species = species(specializedClass);
    var recordType = species.recordType();
    var accessor = Arrays.stream(recordType.getRecordComponents())
        .filter(c -> c.getName().equals(name))
        .findFirst()
        .map(RecordComponent::getAccessor)
        .orElseThrow();
    try {
      return new ConstantCallSite(species.recordLookup().unreflect(accessor).asType(methodType));
    } catch (IllegalAccessException e) {
      throw (IllegalAccessError) new IllegalAccessError().initCause(e);
    }
  }
}
