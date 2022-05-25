package myagent;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.AdviceAdapter;
import org.objectweb.asm.commons.LocalVariablesSorter;

import java.io.IOException;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static org.objectweb.asm.Opcodes.*;

public final class MyAgent {

    public static AtomicInteger numberOfAllCreatedObjects = new AtomicInteger(0);

    public static AtomicInteger numberOfAllClasses = new AtomicInteger(0);

    public static Queue<String> loadedClassNames = new ConcurrentLinkedQueue<>();

    public static void premain(String args, Instrumentation inst) throws IOException, UnmodifiableClassException, ClassNotFoundException {
        System.out.println("AGENT START");
        System.out.println(Object.class);
        System.out.println("my name is " + MyAgent.class.getName());
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
//                return ClassFileTransformer.super.transform(loader, className, classBeingRedefined, protectionDomain, classfileBuffer);
            }
        });

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader,
                                    String className,
                                    Class<?> classBeingRedefined,
                                    ProtectionDomain protectionDomain,
                                    byte[] classfileBuffer) throws IllegalClassFormatException {
                //            System.out.println(className);
                if (!className.equals("nsu/fit/javaperf/TransactionProcessor")) {
                    return null;
                }
                System.out.println("fucking up target class: " + className);
                ClassReader reader = new ClassReader(classfileBuffer);
                ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
                reader.accept(new TransactionProcessorVisitor(ASM7, writer), ClassReader.EXPAND_FRAMES);
                return writer.toByteArray();
            }
        });

//        inst.addTransformer(new ObjectTransformer());
//        inst.addTransformer(new TimerTransformer());

        inst.redefineClasses(
                new ClassDefinition(Object.class, getRedefinedObjectBytes())
//                new ClassDefinition(Class.forName(trProcessorClassName),
//                        getRedefinedTransactionProcessorBytes())
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

    private static class TimerTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader,
                                String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) {
            if (!className.equals("nsu/fit/javaperf")) {
                //return null或者执行异常会执行原来的字节码
                return null;
            }
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            reader.accept(new TimerClassVisitor(writer), ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        }
    }

    public static class TimerClassVisitor extends ClassVisitor {
        public TimerClassVisitor(ClassVisitor classVisitor) {
            super(Opcodes.ASM7, classVisitor);
        }
        @Override
        public MethodVisitor visitMethod(int methodAccess, String methodName, String methodDesc, String signature, String[] exceptions) {
            MethodVisitor methodVisitor = cv.visitMethod(methodAccess, methodName, methodDesc, signature, exceptions);
            return new TimerAdviceAdapter(Opcodes.ASM7, methodVisitor, methodAccess, methodName, methodDesc);
        }
    }

    public static class TimerAdviceAdapter extends AdviceAdapter {
        private String methodName;
        protected TimerAdviceAdapter(int api,
                                     MethodVisitor methodVisitor,
                                     int methodAccess,
                                     String methodName,
                                     String methodDesc) {
            super(api, methodVisitor, methodAccess, methodName, methodDesc);
            this.methodName = methodName;
        }
        @Override
        protected void onMethodEnter() {
            //在方法入口处植入
            if ("<init>".equals(methodName)|| "<clinit>".equals(methodName)) {
                return;
            }
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(".");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(methodName);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKESTATIC, "org/xunche/agent/TimeHolder", "start", "(Ljava/lang/String;)V", false);
        }
        @Override
        protected void onMethodExit(int i) {
            //在方法出口植入
            if ("<init>".equals(methodName) || "<clinit>".equals(methodName)) {
                return;
            }
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(".");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(methodName);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitVarInsn(ASTORE, 1);
            mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
            mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
            mv.visitInsn(DUP);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "()V", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitLdcInsn(": ");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitMethodInsn(INVOKESTATIC, "org/xunche/agent/TimeHolder", "cost", "(Ljava/lang/String;)J", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(J)Ljava/lang/StringBuilder;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
        }
    }

    private static class ObjectTransformer implements ClassFileTransformer {
        @Override
        public byte[] transform(ClassLoader loader,
                                String className,
                                Class<?> classBeingRedefined,
                                ProtectionDomain protectionDomain,
                                byte[] classfileBuffer) {
//            System.out.println(className);
            if (!className.equals("java/lang/Object")) {
                return null;
            }
            System.out.println("fucking up class (if it is not java/lang/object kill me): " + className);
            ClassReader reader = new ClassReader(classfileBuffer);
            ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
            reader.accept(new ObjectClassVisitor(ASM7, writer), ClassReader.EXPAND_FRAMES);
            return writer.toByteArray();
        }
    }

    private static final String trProcessorClassName = "nsu.fit.javaperf.TransactionProcessor";

    private static byte[] getRedefinedTransactionProcessorBytes() throws IOException {
        ClassReader reader = new ClassReader(trProcessorClassName);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        ClassVisitor visitor = new TransactionProcessorVisitor(ASM7, writer);

        reader.accept(visitor, ClassReader.EXPAND_FRAMES);

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

//            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
//                    "nsu/fit/javaperf/TransactionProcessor",
//                    "printHehe",
//                    "()V",
//                    false);

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
                // injecting for java.lang.Object.<init>
                return new AddTimerMethodAdapter4(i, s1, visitor);
            }
            return visitor;
        }
    }

    static class AddTimerMethodAdapter4 extends LocalVariablesSorter {
        private int time;
        private String owner = "nsu/fit/javaperf/TransactionProcessor";
        public AddTimerMethodAdapter4(int access, String desc,
                                      MethodVisitor mv) {
            super(ASM7, access, desc, mv);
        }
        @Override public void visitCode() {
            super.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/System",
                    "currentTimeMillis", "()J", false);
            time = newLocal(Type.LONG_TYPE);
            mv.visitVarInsn(LSTORE, time);
        }
        @Override public void visitInsn(int opcode) {
            if ((opcode >= IRETURN && opcode <= RETURN) || opcode == ATHROW) {
                //===
                mv.visitFieldInsn(Opcodes.GETSTATIC,
                        "java/lang/System",
                        "out",
                        "Ljava/io/PrintStream;");
                mv.visitMethodInsn(INVOKESTATIC, "java/lang/System",
                        "currentTimeMillis", "()J", false);
                mv.visitVarInsn(LLOAD, time);
                mv.visitInsn(LSUB);
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                        "java/io/PrintStream",
                        "println",
                        "(J)V",
                        false);
                //===
//                mv.visitFieldInsn(GETSTATIC, owner, "timer", "J");
//                mv.visitInsn(LADD);
//                mv.visitFieldInsn(PUTSTATIC, owner, "timer", "J");
            }
            super.visitInsn(opcode);
        }
        @Override public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(maxStack + 4, maxLocals);
        }
    }

//    static class TransactionProcessorMethodVisitor extends MethodVisitor {
//        public TransactionProcessorMethodVisitor(int i, MethodVisitor methodVisitor) {
//            super(i, methodVisitor);
//        }
//
//        @Override
//        public void visitCode() {
//            //TODO measure time
////            super.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
////            super.visitLdcInsn("Method");
////            super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
//            super.visitCode();
//        }
//
//        @Override
//        public AnnotationVisitor visitTypeAnnotation(final int typeRef,
//                                                     final TypePath typePath,
//                                                     final String descriptor,
//                                                     final boolean visible) {
//
//            super.visitTypeAnnotation(
//                    typeRef,
//                    typePath,
//                    descriptor,
//                    visible
//            );
//            System.out.println(typePath);
//            System.out.println(descriptor);
//            return null;
////            return new AnnotationVisitor(ASM7, null) {
////                @Override
////                public
////            };
//        }
//
//        @Override
//        public void visitEnd() {
//            super.visitEnd();
//        }
//    }

}
