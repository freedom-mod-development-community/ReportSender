package xyz.fmdc.reportsender.mod.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;
import xyz.fmdc.reportsender.mod.HookRegistry;

import java.lang.reflect.Field;

/**
 * フックの差し込み
 *
 * @author anatawa12
 * @author noyciy7037
 */
public class RSMCoreTransformer implements IClassTransformer {
    @SuppressWarnings("PointlessBitwiseExpression")
    private static final int ASM4 = 4 << 16 | 0 << 8 | 0;
    @SuppressWarnings("PointlessBitwiseExpression")
    private static final int ASM5 = 5 << 16 | 0 << 8 | 0;
    static int ASM4OR5;

    static {
        HookRegistry.registration();
        // check ASM4 is supported
        new MethodNode(ASM4);
        try {
            Field apiField = MethodVisitor.class.getDeclaredField("api");
            apiField.setAccessible(true);
            int api = (int) (Integer) apiField.get(new MethodNode());
            if (api == ASM4) {
                ASM4OR5 = ASM4;
            } else {
                // is asm4 is supported and current version is not asm4, this means asm 5 or later.
                // so use ASM5
                ASM4OR5 = ASM5;
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (!"net.minecraft.crash.CrashReport".equals(transformedName)) return basicClass;
        ClassReader reader = new ClassReader(basicClass);
        ClassWriter cw = new ClassWriter(0);
        reader.accept(new ClassTransformer(cw), 0);
        return cw.toByteArray();
    }

    // mapping: func_147149_a:saveToFile
    // mapping: func_71502_e:getCompleteReport
    // mapping: field_71510_d:crashReportFile

    static class ClassTransformer extends ClassVisitor {
        ClassTransformer(ClassVisitor cv) {
            super(ASM4OR5, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if ("(Ljava/io/File;)Z".equals(desc)) {
                if ("func_147149_a".equals(name))
                    return new MethodTransformer(super.visitMethod(access, name, desc, signature, exceptions), true);
                else if ("saveToFile".equals(name))
                    return new MethodTransformer(super.visitMethod(access, name, desc, signature, exceptions), false);
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    static class MethodTransformer extends MethodVisitor {
        final boolean isObf;

        MethodTransformer(MethodVisitor mv, boolean isObf) {
            super(ASM4OR5, mv);
            this.isObf = isObf;
        }

        private String obf(String srg, String mcp) {
            return isObf ? srg : mcp;
        }

        @Override
        public void visitCode() {
            super.visitCode();
            visitVarInsn(Opcodes.ALOAD, 0);
            visitFieldInsn(
                    Opcodes.GETFIELD,
                    "net/minecraft/crash/CrashReport",
                    obf("field_71510_d", "crashReportFile"),
                    "Ljava/io/File;"
            );
            visitVarInsn(Opcodes.ALOAD, 1);
            visitVarInsn(Opcodes.ALOAD, 0);
            visitMethodInsn(
                    Opcodes.INVOKEVIRTUAL,
                    "net/minecraft/crash/CrashReport",
                    obf("func_71502_e", "getCompleteReport"),
                    "()Ljava/lang/String;"
            );
            visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    "xyz/fmdc/reportsender/mod/asm/RSMCoreHook",
                    "crash",
                    "(Ljava/io/File;Ljava/io/File;Ljava/lang/String;)V"
            );
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            super.visitMaxs(Math.max(maxStack, 3), maxLocals);
        }
    }
}