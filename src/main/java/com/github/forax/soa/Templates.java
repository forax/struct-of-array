package com.github.forax.soa;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static org.objectweb.asm.Opcodes.*;

final class Templates {
  private Templates() {}

  record RecordComponent(String name, Class<?> type) { }

  private static String arrayDescriptor(Class<?> componentType) {
    return '[' + componentType.descriptorString();
  }
  private static boolean requireGenericUpcast(Class<?> componentType) {
    return !componentType.isPrimitive() && componentType != Objects.class;
  }
  private static String internalName(Class<?> componentType) {
    return Type.getInternalName(componentType);
  }
  private static int newArrayKind(Class<?> type) {
    return switch (type.descriptorString()) {
      case "Z" -> T_BOOLEAN;
      case "C" -> T_CHAR;
      case "F" -> T_FLOAT;
      case "D" -> T_DOUBLE;
      case "B" -> T_BYTE;
      case "S" -> T_SHORT;
      case "I" -> T_INT;
      case "J" -> T_LONG;
      default -> throw new AssertionError("invalid type " + type);
    };
  }


  static void templateListInitCanonical(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    // 0: aload_0
    // 1: iload_1
    // 2: iload_2
    // 3: invokespecial #1                  // Method com/github/forax/soa/StructOfArrayList."<init>":(IZ)V

    // 6: aload_0
    // 7: aload_3
    // 8: putfield      #7                  // Field array0:[I

    // 11: aload_0
    // 12: aload         4
    // 14: putfield      #13                 // Field array1:[Ljava/lang/String;

    // 17: return

    var parameterStart = 3;

    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitMethodInsn(INVOKESPECIAL, "com/github/forax/soa/StructOfArrayList", "<init>", "(IZ)V", false);

    for (var i = 0; i < components.size(); i++) {
      var component = components.get(i);
      var componentType = component.type();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, parameterStart + i);
      mv.visitFieldInsn(PUTFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
    }
    mv.visitInsn(RETURN);
  }

  static void templateListInitDefault(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    //  0: aload_0
    //  1: iconst_0
    //  2: iload_2

    //  3: iload_1
    //  4: newarray       int

    //  6: iload_1
    //  7: anewarray     #17                 // class java/lang/String

    // 10: invokespecial #19                 // Method "<init>":(IZ[I[Ljava/lang/String;)V
    // 13: return

    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ICONST_0);
    mv.visitVarInsn(ILOAD, 2);

    for(var component: components) {
      var componentType = component.type();
      mv.visitVarInsn(ILOAD, 1);
      if (componentType.isPrimitive()) {
        mv.visitIntInsn(NEWARRAY, newArrayKind(componentType));
      } else {
        mv.visitTypeInsn(ANEWARRAY, internalName(componentType));
      }
    }
    mv.visitMethodInsn(INVOKESPECIAL, specializedClassName, "<init>",
        "(IZ" + components.stream().map(c -> arrayDescriptor(c.type())).collect(Collectors.joining()) + ")V",
        false);
    mv.visitInsn(RETURN);
  }

  static void templateGetValue(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    //  3: new           #28                 // class com/github/forax/soa/Person
    //  6: dup

    //  7: aload_0
    //  8: getfield      #7                  // Field array0:[I
    // 11: iload_1
    // 12: iaload

    // 13: aload_0
    // 14: getfield      #13                 // Field array1:[Ljava/lang/String;
    // 17: iload_1
    // 18: aaload

    // 19: invokespecial #30                 // Method com/github/forax/soa/Person."<init>":(ILjava/lang/String;)V
    // 22: astore_2

    for (var i = 0; i < components.size(); i++) {
      var component = components.get(i);
      var componentType = component.type();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
      mv.visitVarInsn(ILOAD, 1);
      mv.visitInsn(Type.getType(componentType).getOpcode(IALOAD));
    }

    mv.visitInvokeDynamicInsn("new",
        "(" + components.stream().map(c -> c.type().descriptorString()).collect(joining()) + ")Ljava/lang/Object;",
        BSM);
    mv.visitVarInsn(ASTORE, 2);
  }

  static void templateSetValue(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    //  8: aload_0
    //  9: getfield      #7                  // Field array0:[I
    // 12: iload_1
    // 13: aload_3
    // 14: invokevirtual #36                 // Method com/github/forax/soa/Person.age:()I
    // 17: iastore

    // 18: aload_0
    // 19: getfield      #13                 // Field array1:[Ljava/lang/String;
    // 22: iload_1
    // 23: aload_3
    // 24: invokevirtual #40                 // Method com/github/forax/soa/Person.name:()Ljava/lang/String;
    // 27: aastore

    for (int i = 0; i < components.size(); i++) {
      var component = components.get(i);
      var componentType = component.type();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
      mv.visitVarInsn(ILOAD, 1);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitInvokeDynamicInsn(component.name(), "(Ljava/lang/Object;)" + componentType.descriptorString(), BSM_RECORD_ACCESS);
      mv.visitInsn(Type.getType(componentType).getOpcode(IASTORE));
    }
  }

  static void templateListCopyElement(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    //  3: aload_0
    //  4: getfield      #7                  // Field array0:[I
    //  7: iload_1
    //  8: aload_0
    //  9: getfield      #7                  // Field array0:[I
    // 12: iload_2
    // 13: iaload
    // 14: iastore

    // 15: aload_0
    // 16: getfield      #13                 // Field array1:[Ljava/lang/String;
    // 19: iload_1
    // 20: aload_0
    // 21: getfield      #13                 // Field array1:[Ljava/lang/String;
    // 24: iload_2
    // 25: aaload
    // 26: aastore

    for (var i = 0; i < components.size(); i++) {
      var component = components.get(i);
      var componentType = component.type();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
      mv.visitVarInsn(ILOAD, 1);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
      mv.visitVarInsn(ILOAD, 2);
      mv.visitInsn(Type.getType(componentType).getOpcode(IALOAD));
      mv.visitInsn(Type.getType(componentType).getOpcode(IASTORE));
    }
  }

  static void templateListZeroElement(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    //  3: aload_0
    //  4: getfield      #13                 // Field array1:[Ljava/lang/String;
    //  7: iload_1
    //  8: aconst_null
    //  9: aastore

    for (var i = 0; i < components.size(); i++) {
      var component = components.get(i);
      var componentType = component.type();
      if (componentType.isPrimitive()) {
        continue;
      }
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
      mv.visitVarInsn(ILOAD, 1);
      mv.visitInsn(ACONST_NULL);
      mv.visitInsn(AASTORE);
    }
  }

  static void templateIndexOfOrContainsMaterialize(MethodVisitor mv, List<RecordComponent> components) {
    // 22: aload_2
    // 23: invokevirtual #36                 // Method com/github/forax/soa/Person.age:()I
    // 26: istore        4

    // 27: aload_2
    // 28: invokevirtual #40                 // Method com/github/forax/soa/Person.name:()Ljava/lang/String;
    // 31: astore        5

    var slot = 4;

    for (var component : components) {
      var componentType = component.type();
      mv.visitVarInsn(ALOAD, 2);
      mv.visitInvokeDynamicInsn(component.name(), "(Ljava/lang/Object;)" + componentType.descriptorString(), BSM_RECORD_ACCESS);
      mv.visitVarInsn(Type.getType(componentType).getOpcode(ISTORE), slot);
      slot += (componentType == long.class || componentType == double.class) ? 2 : 1;
    }
  }

  static void templateIndexOfOrContainsEquals(MethodVisitor mv, String specializedClassName, List<RecordComponent> components, boolean indexResult) {
    // 55: aload_0
    // 56: getfield      #7                  // Field array0:[I
    // 59: iload_3
    // 60: iaload
    // 61: iload         4
    // 63: if_icmpne     82

    // 66: aload_0
    // 67: getfield      #13                 // Field array1:[Ljava/lang/String;
    // 70: iload_3
    // 71: aaload
    // 72: aload         5
    // 74: invokestatic  #73                 // Method java/util/Objects.equals:(Ljava/lang/Object;Ljava/lang/Object;)Z
    // 77: ifeq          82

    // 80: iload_3
    // 81: ireturn

    var slot = 4;

    var endLabel = new Label();

    for (var i = 0; i < components.size(); i++) {
      var component = components.get(i);
      var componentType = component.type();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
      mv.visitVarInsn(ILOAD, 3);
      mv.visitInsn(Type.getType(componentType).getOpcode(IALOAD));
      mv.visitVarInsn(Type.getType(componentType).getOpcode(ILOAD), slot);
      slot += (componentType == long.class || componentType == double.class)? 2: 1;
      switch (componentType.descriptorString()) {
        case "Z", "B", "C", "S", "I", "J" -> mv.visitJumpInsn(IF_ICMPNE, endLabel);
        case "F" -> throw new AssertionError("FIXME");  // FIXME
        case "D" -> throw new AssertionError("FIXME");  // FIXME
        default -> {
          mv.visitMethodInsn(INVOKESTATIC, "java/util/Objects", "equals",
              "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
          mv.visitJumpInsn(IFEQ, endLabel);
        }
      }
    }

    if (indexResult) {
      mv.visitVarInsn(ILOAD, 3);  // return index
    } else {
      mv.visitInsn(ICONST_1);  // return true;
    }
    mv.visitInsn(IRETURN);
    mv.visitLabel(endLabel);
  }

  static void templateCopyAll(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    //  3: aload_0
    //  4: aload_0
    //  5: getfield      #7                  // Field array0:[I
    //  8: iload_1
    //  9: invokestatic  #85                 // Method java/util/Arrays.copyOf:([II)[I
    // 12: putfield      #7                  // Field array0:[I

    // 15: aload_0
    // 16: aload_0
    // 17: getfield      #13                 // Field array1:[Ljava/lang/String;
    // 20: iload_1
    // 21: invokestatic  #91                 // Method java/util/Arrays.copyOf:([Ljava/lang/Object;I)[Ljava/lang/Object;
    // 24: checkcast     #94                 // class "[Ljava/lang/String;"
    // 27: putfield      #13                 // Field array1:[Ljava/lang/String;

    for (var i = 0; i < components.size(); i++) {
      var component = components.get(i);
      var componentType = component.type();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
      mv.visitVarInsn(ILOAD, 1);
      var erasedArrayType = componentType.isPrimitive()? componentType.arrayType(): Object[].class;
      mv.visitMethodInsn(INVOKESTATIC, "java/util/Arrays", "copyOf",
          "(" + erasedArrayType.descriptorString() + "I)" + erasedArrayType.descriptorString(),
          false);
      if (requireGenericUpcast(componentType)) {
        mv.visitTypeInsn(CHECKCAST, arrayDescriptor(componentType));
      }
      mv.visitFieldInsn(PUTFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
    }
  }

  static void templateListAddResize(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    // 28: aload_0
    // 29: getfield      #52                 // Field size:I
    // 32: aload_0
    // 33: getfield      #7                  // Field array0:[I
    // 36: arraylength
    // 37: if_icmpne     44
    // 40: aload_0
    // 41: invokevirtual #89                 // Method resize:()V

    if (components.isEmpty()) {
      return;  // no component, no resize needed
    }
    var firstComponentType = components.get(0).type();

    var endLabel = new Label();

    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, specializedClassName, "size", "I");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, specializedClassName, "array0", arrayDescriptor(firstComponentType));
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IF_ICMPNE, endLabel);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, specializedClassName, "resize", "()V", false);
    mv.visitLabel(endLabel);
  }

  static void templateListClear(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    // 18: aload_0
    // 19: iconst_0
    // 20: newarray       int
    // 22: putfield      #7                  // Field array0:[I

    // 25: aload_0
    // 26: iconst_0
    // 27: anewarray     #17                 // class java/lang/String
    // 30: putfield      #13                 // Field array1:[Ljava/lang/String;

    for (var i = 0; i < components.size(); i++) {
      var component = components.get(i);
      var componentType = component.type();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(ICONST_0);
      if (componentType.isPrimitive()) {
        mv.visitIntInsn(NEWARRAY, newArrayKind(componentType));
      } else {
        mv.visitTypeInsn(ANEWARRAY, internalName(componentType));
      }
      mv.visitFieldInsn(PUTFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
    }
  }

  static void templateMapInit(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    //  8: aload_0
    //  9: iload_1
    // 10: newarray       int
    // 12: putfield      #13                 // Field array0:[I

    // 15: aload_0
    // 16: iload_1
    // 17: anewarray     #19                 // class java/lang/String
    // 20: putfield      #21                 // Field array1:[Ljava/lang/String;

    for (int i = 0; i < components.size(); i++) {
      var component = components.get(i);
      var componentType = component.type();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ILOAD, 1);
      if (componentType.isPrimitive()) {
        mv.visitIntInsn(NEWARRAY, newArrayKind(componentType));
      } else {
        mv.visitTypeInsn(ANEWARRAY, internalName(componentType));
      }
      mv.visitFieldInsn(PUTFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
    }
  }

  static void templateMapClear(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    // 19: aload_0
    // 20: bipush        16
    // 22: newarray       int
    // 24: putfield      #13                 // Field array0:[I

    // 27: aload_0
    // 28: bipush        16
    // 30: anewarray     #19                 // class java/lang/String
    // 33: putfield      #21                 // Field array1:[Ljava/lang/String;

    for (var i = 0; i < components.size(); i++) {
      var component = components.get(i);
      var componentType = component.type();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitIntInsn(BIPUSH, 16);
      if (componentType.isPrimitive()) {
        mv.visitIntInsn(NEWARRAY, newArrayKind(componentType));
      } else {
        mv.visitTypeInsn(ANEWARRAY, internalName(componentType));
      }
      mv.visitFieldInsn(PUTFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
    }
  }

  static final Handle BSM = new Handle(H_INVOKESTATIC, "com/github/forax/soa/RT", "bsm",
      "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
      false);

  private static final Handle BSM_RECORD_ACCESS = new Handle(H_INVOKESTATIC, "com/github/forax/soa/RT", "bsm_record_access",
      "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
      false);

  static void templateMapValues(MethodVisitor mv, String specializedClassName, List<RecordComponent> components) {
    //  3: new           #81                 // class com/github/forax/soa/StructOfArrayList$Template
    //  6: dup
    //  7: aload_0
    //  8: getfield      #47                 // Field size:I
    // 11: iconst_1

    // 12: aload_0
    // 13: getfield      #13                 // Field array0:[I

    // 16: aload_0
    // 17: getfield      #21                 // Field array1:[Ljava/lang/String;

    // 20: invokespecial #83                 // Method com/github/forax/soa/StructOfArrayList$Template."<init>":(IZ[I[Ljava/lang/String;)V
    // 23: astore_1

    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, specializedClassName, "size", "I");
    mv.visitInsn(ICONST_1);

    for (var i = 0; i < components.size(); i++) {
      var component = components.get(i);
      var componentType = component.type();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, specializedClassName, "array" + i, arrayDescriptor(componentType));
    }

    mv.visitInvokeDynamicInsn("newCanonicalList",
        "(IZ" + components.stream().map(c -> arrayDescriptor(c.type())).collect(joining()) + ")Lcom/github/forax/soa/StructOfArrayList;",
        BSM);
    mv.visitVarInsn(ASTORE, 1);
  }
}
