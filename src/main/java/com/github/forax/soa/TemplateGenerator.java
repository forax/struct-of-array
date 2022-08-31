package com.github.forax.soa;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.util.CheckClassAdapter;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.joining;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ASM9;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INSTANCEOF;

final class TemplateGenerator {
  public static void main(String[] args) throws IOException {
    //var generator = TemplateGenerator.specialized(StructOfArrayList$Template.class, Person.class);
    var generator = TemplateGenerator.specialized(StructOfArrayMap$Template.class, Person.class);
    var bytecode = generator.generate();
    Files.write(Path.of(generator.specializedClassName.replace('/', '_')+".class"), bytecode);
  }

  private static byte[] templateBytecode(Class<?> template) {
    var name = "/" + template.getName().replace('.', '/') + ".class";
    byte[] data;
    try(var input = template.getResourceAsStream(name)) {
      if (input == null) {
        throw new LinkageError("can not resource " + name);
      }
      return input.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private static Class<?> erase(Class<?> type) {
    return type.isPrimitive()? type: Object.class;
  }

  public static TemplateGenerator specialized(Class<?> template, Class<?> recordType) {
    Objects.requireNonNull(template, "template is null");
    Objects.requireNonNull(recordType, "record type is null");
    if (!recordType.isRecord()) {
      throw new IllegalArgumentException(recordType.getName() + " not a record");
    }
    var templateBytecode = templateBytecode(template);
    var recordMangledName = recordType.getName().replace('.', '_');
    var specializedClassName = template.getName().replace('.', '/') + '$' + recordMangledName;
    var components =
        Arrays.stream(recordType.getRecordComponents())
            .map(c -> new Templates.RecordComponent(c.getName(), erase(c.getType())))
            .toList();
    return new TemplateGenerator(templateBytecode, specializedClassName, components);
  }

  private final byte[] templateBytecode;
  private final String specializedClassName;
  private final List<Templates.RecordComponent> components;

  private TemplateGenerator(byte[] templateBytecode, String specializedClassName, List<Templates.RecordComponent> components) {
    this.templateBytecode = templateBytecode;
    this.specializedClassName = specializedClassName;
    this.components = components;
  }

  private void insertSnippet(MethodVisitor mv, String className, String methodName, String methodDescriptor, int snippetNumber) {
    var mangled = className + "." + methodName + methodDescriptor + snippetNumber;
    switch (mangled) {
      case "com/github/forax/soa/StructOfArrayList$Template.<init>0" -> {
        Templates.templateListInitCanonical(mv, specializedClassName, components);
      }
      case "com/github/forax/soa/StructOfArrayList$Template.<init>1" -> {
        Templates.templateListInitDefault(mv, specializedClassName, components);
      }
      case "com/github/forax/soa/StructOfArrayList$Template.valueAt(I)Ljava/lang/Object;0",
           "com/github/forax/soa/StructOfArrayMap$Template.valueAt(I)Ljava/lang/Object;0" -> {
        Templates.templateGetValue(mv, specializedClassName, components);
      }
      case "com/github/forax/soa/StructOfArrayList$Template.valueAt(ILjava/lang/Object;)V0",
           "com/github/forax/soa/StructOfArrayMap$Template.valueAt(ILjava/lang/Object;)V0" -> {
        Templates.templateSetValue(mv, specializedClassName, components);
      }
      case "com/github/forax/soa/StructOfArrayList$Template.copyElement(II)V0",
           "com/github/forax/soa/StructOfArrayMap$Template.copyElement(II)V0" -> {
        Templates.templateListCopyElement(mv, specializedClassName, components);
      }
      case "com/github/forax/soa/StructOfArrayList$Template.zeroElement(I)V0",
           "com/github/forax/soa/StructOfArrayMap$Template.zeroElement(I)V0" -> {
        Templates.templateListZeroElement(mv, specializedClassName, components);
      }
      case "com/github/forax/soa/StructOfArrayList$Template.indexOf(Ljava/lang/Object;)I0",
           "com/github/forax/soa/StructOfArrayList$Template.lastIndexOf(Ljava/lang/Object;)I0",
           "com/github/forax/soa/StructOfArrayMap$Template.containsValue(Ljava/lang/Object;)Z0" -> {
        Templates.templateIndexOfOrContainsMaterialize(mv, components);
      }
      case "com/github/forax/soa/StructOfArrayList$Template.indexOf(Ljava/lang/Object;)I1",
           "com/github/forax/soa/StructOfArrayList$Template.lastIndexOf(Ljava/lang/Object;)I1" -> {
        Templates.templateIndexOfOrContainsEquals(mv, specializedClassName, components, true);
      }
      case "com/github/forax/soa/StructOfArrayMap$Template.containsValue(Ljava/lang/Object;)Z1" -> {
        Templates.templateIndexOfOrContainsEquals(mv, specializedClassName, components, false);
      }
      case "com/github/forax/soa/StructOfArrayList$Template.copyAll(I)V0",
           "com/github/forax/soa/StructOfArrayMap$Template.copyAll(I)V0" -> {
        Templates.templateCopyAll(mv, specializedClassName, components);
      }
      case "com/github/forax/soa/StructOfArrayList$Template.add(Ljava/lang/Object;)Z0" -> {
        Templates.templateListAddResize(mv, specializedClassName, components);
      }
      case "com/github/forax/soa/StructOfArrayList$Template.clear()V0" -> {
        Templates.templateListClear(mv, specializedClassName, components);
      }
      case "com/github/forax/soa/StructOfArrayMap$Template.<init>(I)V0" -> {
        Templates.templateMapInit(mv, specializedClassName, components);
      }
      case "com/github/forax/soa/StructOfArrayMap$Template.clear()V0" -> {
        Templates.templateMapClear(mv, specializedClassName, components);
      }
      case "com/github/forax/soa/StructOfArrayMap$Template.values()Lcom/github/forax/soa/StructOfArrayList;0" -> {
        Templates.templateMapValues(mv, specializedClassName, components);
      }
      default -> throw new AssertionError("no snippet " + mangled);
    }
  }

  public byte[] generate() {
    var reader = new ClassReader(templateBytecode);
    var writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);
    var checker = new CheckClassAdapter(writer, true);
    var renamer = new ClassRemapper(checker, new Remapper() {
      @Override
      public String mapType(String internalName) {
        assert !internalName.equals("com/github/forax/soa/Person"): "Person should never leak";
        assert !internalName.equals("com/github/forax/soa/Snippets"): "Snippets should never leak";

        if (internalName.endsWith("$Template")) {
          return specializedClassName;
        }
        return super.mapType(internalName);
      }
    });
    reader.accept(new ClassVisitor(ASM9, renamer) {
      private String className;

      @Override
      public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, specializedClassName, signature, superName, interfaces);

        // add fields
        for (var i = 0; i < components.size(); i++) {
          var component = components.get(i);
          var componentType = component.type();
          super.visitField(ACC_PRIVATE, "array" + i, "[" + componentType.descriptorString(), null, null);
        }
      }

      private void replaceListConstructors(int access, String methodName, String methodDescriptor) {
        int snippetNumber;
        String initMethodDescriptor;
        if (methodDescriptor.equals("(IZ)V")) {
          // default constructor
          if (components.isEmpty()) {
            return;  // the canonical constructor is also the default constructor
          }
          snippetNumber = 1;
          initMethodDescriptor = "(IZ)V";
        } else {
          // canonical constructor
          snippetNumber = 0;
          initMethodDescriptor = "(IZ" + components.stream().map(c -> "[" + c.type().descriptorString()).collect(joining())+ ")V";
        }
        var mv = super.visitMethod(access, methodName, initMethodDescriptor, null, null);
        mv.visitCode();
        insertSnippet(mv, className, methodName, "", snippetNumber);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
      }

      @Override
      public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        return null;  // skip existing fields
      }

      @Override
      public MethodVisitor visitMethod(int access, String methodName, String methodDescriptor, String signature, String[] exceptions) {
        // replace constructors of StructOfArrayList$Template
        if (methodName.equals("<init>") && className.equals("com/github/forax/soa/StructOfArrayList$Template")) {
          replaceListConstructors(access, methodName, methodDescriptor);
          return null;
        }

        // replace snippets
        var mv = super.visitMethod(access, methodName, methodDescriptor, signature, exceptions);
        return new MethodVisitor(ASM9, mv) {
          private boolean insideSnippet;
          private int snippetNumber;

          @Override
          public void visitParameter(String name, int access) {
            super.visitParameter(name, access);
          }

          @Override
          public AnnotationVisitor visitAnnotationDefault() {
            return super.visitAnnotationDefault();
          }

          @Override
          public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            return super.visitAnnotation(descriptor, visible);
          }

          @Override
          public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
          }

          @Override
          public void visitAnnotableParameterCount(int parameterCount, boolean visible) {
            super.visitAnnotableParameterCount(parameterCount, visible);
          }

          @Override
          public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
            return super.visitParameterAnnotation(parameter, descriptor, visible);
          }

          @Override
          public void visitAttribute(Attribute attribute) {
            super.visitAttribute(attribute);
          }

          @Override
          public void visitCode() {
            super.visitCode();
          }

          @Override
          public void visitFrame(int type, int numLocal, Object[] local, int numStack, Object[] stack) {
            // do not visit frame, they are reconstructed by the ClassWriter
          }

          @Override
          public void visitInsn(int opcode) {
            if (insideSnippet) {
              return;
            }
            super.visitInsn(opcode);
          }

          @Override
          public void visitIntInsn(int opcode, int operand) {
            if (insideSnippet) {
              return;
            }
            super.visitIntInsn(opcode, operand);
          }

          @Override
          public void visitVarInsn(int opcode, int varIndex) {
            if (insideSnippet) {
              return;
            }
            super.visitVarInsn(opcode, varIndex);
          }

          @Override
          public void visitTypeInsn(int opcode, String type) {
            if (insideSnippet) {
              return;
            }
            // reference to the record type should not be present in the bytecode
            if ((opcode == INSTANCEOF || opcode == CHECKCAST) && type.equals("com/github/forax/soa/Person")) {
              super.visitInvokeDynamicInsn(
                  opcode == INSTANCEOF? "instanceof": "checkcast",
                  opcode == INSTANCEOF? "(Ljava/lang/Object;)Z": "(Ljava/lang/Object;)Ljava/lang/Object;",
                  Templates.BSM);
              return;
            }
            super.visitTypeInsn(opcode, type);
          }

          @Override
          public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
            if (insideSnippet) {
              return;
            }
            super.visitFieldInsn(opcode, owner, name, descriptor);
          }

          @Override
          public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (owner.equals(Snippets.class.getName().replace('.', '/'))) {
              insideSnippet = !insideSnippet;
              if (name.equals("end")) {
                insertSnippet(mv, className, methodName, methodDescriptor, snippetNumber);
                snippetNumber++;
              }
              return;
            }

            if (insideSnippet) {
              return;
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
          }

          @Override
          public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrapMethodHandle, Object... bootstrapMethodArguments) {
            if (insideSnippet) {
              return;
            }
            super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, bootstrapMethodArguments);
          }

          @Override
          public void visitJumpInsn(int opcode, Label label) {
            if (insideSnippet) {
              return;
            }
            super.visitJumpInsn(opcode, label);
          }

          @Override
          public void visitLabel(Label label) {
            if (insideSnippet) {
              return;
            }
            super.visitLabel(label);
          }

          @Override
          public void visitLdcInsn(Object value) {
            if (insideSnippet) {
              return;
            }
            super.visitLdcInsn(value);
          }

          @Override
          public void visitIincInsn(int varIndex, int increment) {
            if (insideSnippet) {
              return;
            }
            super.visitIincInsn(varIndex, increment);
          }

          @Override
          public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            if (insideSnippet) {
              return;
            }
            super.visitTableSwitchInsn(min, max, dflt, labels);
          }

          @Override
          public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
            if (insideSnippet) {
              return;
            }
            super.visitLookupSwitchInsn(dflt, keys, labels);
          }

          @Override
          public void visitMultiANewArrayInsn(String descriptor, int numDimensions) {
            if (insideSnippet) {
              return;
            }
            super.visitMultiANewArrayInsn(descriptor, numDimensions);
          }

          @Override
          public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            if (insideSnippet) {
              return null;
            }
            return super.visitInsnAnnotation(typeRef, typePath, descriptor, visible);
          }

          @Override
          public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            if (insideSnippet) {
              return;
            }
            super.visitTryCatchBlock(start, end, handler, type);
          }

          @Override
          public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
            if (insideSnippet) {
              return null;
            }
            return super.visitTryCatchAnnotation(typeRef, typePath, descriptor, visible);
          }

          @Override
          public void visitLocalVariable(String name, String descriptor, String signature, Label start, Label end, int index) {
            // remove local variable declaration because the generated code may or may not create a local variable
            // and I do not want to track that
          }

          @Override
          public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String descriptor, boolean visible) {
            if (insideSnippet) {
              return null;
            }
            return super.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, descriptor, visible);
          }

          @Override
          public void visitLineNumber(int line, Label start) {
            if (insideSnippet) {
              return;
            }
            super.visitLineNumber(line, start);
          }

          @Override
          public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack, maxLocals);
          }

          @Override
          public void visitEnd() {
            super.visitEnd();
          }
        };
      }
    }, 0);
    return writer.toByteArray();
  }
}
