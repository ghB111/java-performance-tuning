package myagent;

import org.objectweb.asm.*;

import java.io.IOException;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.ASM7;

public class MyAgent {

    public static AtomicInteger numberOfAllCreatedObjects = new AtomicInteger(0);

    public static AtomicInteger numberOfAllClasses = new AtomicInteger(0);

    public static Queue<String> loadedClassNames = new ConcurrentLinkedQueue<>();

    public static void premain(String args, Instrumentation inst) throws IOException, UnmodifiableClassException, ClassNotFoundException {
        System.out.println("AGENT START");
//        System.out.println(MyAgent.class);
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                numberOfAllClasses.incrementAndGet();
                loadedClassNames.add(className);
                return ClassFileTransformer.super.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
            }
        });

        inst.redefineClasses(
                new ClassDefinition(Object.class, getRedefinedObjectBytes()),
                new ClassDefinition(Class.forName(trProcessorClassName),
                        getRedefinedTransactionProcessorBytes())
        );
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Total classes loaded :" + numberOfAllClasses);
            System.out.println("Total news:" + numberOfAllCreatedObjects);
            System.out.println("Classes loaded:");
            for (String className : loadedClassNames) {
                System.out.println(className);
            }
        }));
    }

    private static final String trProcessorClassName = "nsu.fit.javaperf.TransactionProcessor";

    private static byte[] getRedefinedTransactionProcessorBytes() throws IOException {
        ClassReader reader = new ClassReader(trProcessorClassName);
        ClassWriter writer = new ClassWriter(0);
        ClassVisitor visitor = new TransactionProcessorVisitor(ASM7, writer);

        reader.accept(visitor, 0);

        return writer.toByteArray();
    }

    public static void printNumberOfAllClassesAndObjects() {
        System.out.print("Number of all classes:");
        System.out.println(numberOfAllClasses);
        System.out.print("Number of all created objects");
        System.out.println(numberOfAllCreatedObjects);
    }

    private static byte[] getRedefinedObjectBytes() throws IOException {
        ClassReader reader = new ClassReader("java.lang.Object");
        ClassWriter writer = new ClassWriter(0);
        ClassVisitor visitor = new ObjectClassVisitor(ASM7, writer);

        reader.accept(visitor, 0);

        return writer.toByteArray();
    }

    static class ObjectClassVisitor extends ClassVisitor {
        public ObjectClassVisitor(int i, ClassVisitor classVisitor) {
            super(i, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
            MethodVisitor visitor = super.visitMethod(i, s, s1, s2, strings);
            if (!s.equals("<init>")) {
                // injecting for java.lang.Object.<init>
                return new ObjectMethodVisitor(api, visitor);
            }
            return visitor;
        }
    }

    static class ObjectMethodVisitor extends MethodVisitor {
        public ObjectMethodVisitor(int i, MethodVisitor methodVisitor) {
            super(i, methodVisitor);
        }

        @Override
        public void visitCode() {
            super.visitCode();
            super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            super.visitLdcInsn("An object created");
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

            super.visitFieldInsn(Opcodes.GETSTATIC, "myagent/MyAgent",
                    "numberOfAllCreatedObjects",
                    "Ljava/util/concurrent/atomic/AtomicInteger;");
            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                    "java/util/concurrent/atomic/AtomicInteger",
                    "incrementAndGet",
                    "()I",
                    false);

//            super.visitFieldInsn(Opcodes.GETSTATIC, "myagent/MyAgent",
//                    "numberOfAllCreatedObjects",
//                    "Ljava/util/concurrent/atomic/AtomicInteger;");
//            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
//                    "java/util/concurrent/atomic/AtomicInteger",
//                    "incrementAndGet",
//                    "()I",
//                    false);

        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }
    }

    private static class TransactionProcessorVisitor extends ClassVisitor {
        public TransactionProcessorVisitor(int i, ClassVisitor classVisitor) {
            super(i, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
            MethodVisitor visitor = super.visitMethod(i, s, s1, s2, strings);
            if (!s.equals("<init>")) {
                // injecting for java.lang.Object.<init>
                return new TransactionProcessorMethodVisitor(api, visitor);
            }
            return visitor;
        }
    }

    static class TransactionProcessorMethodVisitor extends MethodVisitor {
        public TransactionProcessorMethodVisitor(int i, MethodVisitor methodVisitor) {
            super(i, methodVisitor);
        }

        @Override
        public void visitCode() {
            //TODO measure time
//            super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//            super.visitLdcInsn("Method");
//            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
            super.visitCode();
        }

        @Override
        public AnnotationVisitor visitTypeAnnotation(final int typeRef,
                                                     final TypePath typePath,
                                                     final String descriptor,
                                                     final boolean visible) {

            super.visitTypeAnnotation(
                    typeRef,
                    typePath,
                    descriptor,
                    visible
            );
            System.out.println(typePath);
            System.out.println(descriptor);
            return null;
//            return new AnnotationVisitor(ASM7, null) {
//                @Override
//                public
//            };
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
        }
    }
}
