package myagent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.io.IOException;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public final class MyAgent {

//    public static AtomicInteger numberOfAllCreatedObjects = new AtomicInteger(0);

    public static AtomicInteger numberOfAllClasses = new AtomicInteger(0);

    public static Queue<String> loadedClassNames = new ConcurrentLinkedQueue<>();

    public static void premain(String args, Instrumentation inst) throws IOException, UnmodifiableClassException, ClassNotFoundException {
        System.out.println("AGENT START");

        // count all classes and get their names
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                numberOfAllClasses.incrementAndGet();
                loadedClassNames.add(className);
                return null;
            }
        });

        // add time watches
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                if (!className.equals("nsu/fit/javaperf/TransactionProcessor")) {
                    return null;
                }
                ClassReader reader = new ClassReader(classfileBuffer);
                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                reader.accept(new TransactionProcessorVisitor(ASM7, writer), ClassReader.EXPAND_FRAMES);
                return writer.toByteArray();
            }
        });

        // make all allocations visible
        inst.redefineClasses(
                new ClassDefinition(Object.class, getRedefinedObjectBytes())
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Total classes loaded :" + numberOfAllClasses);
            System.out.println("Classes loaded:");
            for (String className : loadedClassNames) {
                System.out.println(className);
            }
        }));
    }

    private static byte[] getRedefinedObjectBytes() throws IOException {
        ClassReader reader = new ClassReader("java.lang.Object");
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new ObjectClassVisitor(ASM7, writer);

        reader.accept(visitor, ClassReader.EXPAND_FRAMES);

        return writer.toByteArray();
    }

    static class ObjectClassVisitor extends ClassVisitor {
        public ObjectClassVisitor(int i, ClassVisitor classVisitor) {
            super(i, classVisitor);
        }

        @Override
        public MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
            MethodVisitor visitor = cv.visitMethod(i, s, s1, s2, strings);
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
            mv.visitCode();
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn("An object created");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

//            mv.visitFieldInsn(Opcodes.GETSTATIC, "myagent/MyAgent",
//                    "numberOfAllCreatedObjects",
//                    "Ljava/util/concurrent/atomic/AtomicInteger;");
//            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
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
                return new AddTimerMethodAdapter(i, s, s1, visitor);
            }
            return visitor;
        }
    }

    static class AddTimerMethodAdapter extends LocalVariablesSorter {
        private int timeVariable;
        private String methodName;

        public AddTimerMethodAdapter(
                int access,
                String mName,
                String desc,
                MethodVisitor mv
        ) {
            super(ASM7, access, desc, mv);
            methodName = mName;
        }

        @Override public void visitCode() {
            super.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System",
                    "currentTimeMillis", "()J", false);
            timeVariable = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(LSTORE, timeVariable);
        }
        @Override public void visitInsn(int opcode) {
            if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
            mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitLdcInsn(methodName + " took: ");
            mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "print", "(Ljava/lang/String;)V", false);
                //===
                mv.visitFieldInsn(Opcodes.GETSTATIC,
                        "java/lang/System",
                        "out",
                        "Ljava/io/PrintStream;");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System",
                        "currentTimeMillis", "()J", false);
                mv.visitVarInsn(LLOAD, timeVariable);
                mv.visitInsn(LSUB);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        "java/io/PrintStream",
                        "println",
                        "(J)V",
                        false);
            }
            super.visitInsn(opcode);
        }
        @Override public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack + 4, maxLocals);
        }
    }
}
