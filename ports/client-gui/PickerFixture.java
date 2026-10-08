import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Replace only OS chooser replies with deterministic fixtures; game GUI and audio stay real. */
public class PickerFixture implements ClassFileTransformer {
    static volatile boolean tinyReady,sdlReady;
    final Instrumentation instrumentation;
    final String file,folder;
    PickerFixture(Instrumentation i,String file,String folder){instrumentation=i;this.file=file;this.folder=folder;}
    @Override public byte[] transform(Module module,ClassLoader loader,String name,Class<?> redef,ProtectionDomain domain,byte[] bytes){
        if(name==null||!name.equals("org/lwjgl/util/tinyfd/TinyFileDialogs")&&!name.equals("org/lwjgl/sdl/SDLDialog"))return null;
        try{
            ClassNode node=new ClassNode();new ClassReader(bytes).accept(node,0);boolean changed=false;
            for(MethodNode m:node.methods){
                boolean tiny=name.endsWith("TinyFileDialogs")&&m.desc.endsWith("Ljava/lang/String;")&&(m.name.equals("tinyfd_openFileDialog")||m.name.equals("tinyfd_selectFolderDialog"));
                boolean sdl=name.endsWith("SDLDialog")&&m.desc.startsWith("(Lorg/lwjgl/sdl/SDL_DialogFileCallbackI;")&&(m.name.equals("SDL_ShowOpenFileDialog")||m.name.equals("SDL_ShowOpenFolderDialog"));
                if(!tiny&&!sdl)continue;
                m.access &= ~Opcodes.ACC_NATIVE;m.instructions.clear();m.tryCatchBlocks.clear();m.localVariables=null;
                String path=m.name.contains("Folder")?folder:file;
                if(tiny){m.instructions.add(new LdcInsnNode(path));m.instructions.add(new InsnNode(Opcodes.ARETURN));}
                else{
                    // Fabric's Knot loader isolates game classes from the agent. Resolve the fixture through the system loader.
                    m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/ClassLoader","getSystemClassLoader","()Ljava/lang/ClassLoader;",false));
                    m.instructions.add(new LdcInsnNode("GuiScenario"));
                    m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"java/lang/ClassLoader","loadClass","(Ljava/lang/String;)Ljava/lang/Class;",false));
                    m.instructions.add(new LdcInsnNode("fixtureDialog"));
                    m.instructions.add(new InsnNode(Opcodes.ICONST_3));m.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY,"java/lang/Class"));
                    m.instructions.add(new InsnNode(Opcodes.DUP));m.instructions.add(new InsnNode(Opcodes.ICONST_0));m.instructions.add(new LdcInsnNode(Type.getType(Object.class)));m.instructions.add(new InsnNode(Opcodes.AASTORE));
                    m.instructions.add(new InsnNode(Opcodes.DUP));m.instructions.add(new InsnNode(Opcodes.ICONST_1));m.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,"java/lang/Long","TYPE","Ljava/lang/Class;"));m.instructions.add(new InsnNode(Opcodes.AASTORE));
                    m.instructions.add(new InsnNode(Opcodes.DUP));m.instructions.add(new InsnNode(Opcodes.ICONST_2));m.instructions.add(new LdcInsnNode(Type.getType(String.class)));m.instructions.add(new InsnNode(Opcodes.AASTORE));
                    m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"java/lang/Class","getMethod","(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",false));
                    m.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
                    m.instructions.add(new InsnNode(Opcodes.ICONST_3));m.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY,"java/lang/Object"));
                    m.instructions.add(new InsnNode(Opcodes.DUP));m.instructions.add(new InsnNode(Opcodes.ICONST_0));m.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));m.instructions.add(new InsnNode(Opcodes.AASTORE));
                    m.instructions.add(new InsnNode(Opcodes.DUP));m.instructions.add(new InsnNode(Opcodes.ICONST_1));m.instructions.add(new VarInsnNode(Opcodes.LLOAD,1));m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,"java/lang/Long","valueOf","(J)Ljava/lang/Long;",false));m.instructions.add(new InsnNode(Opcodes.AASTORE));
                    m.instructions.add(new InsnNode(Opcodes.DUP));m.instructions.add(new InsnNode(Opcodes.ICONST_2));m.instructions.add(new LdcInsnNode(path));m.instructions.add(new InsnNode(Opcodes.AASTORE));
                    m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,"java/lang/reflect/Method","invoke","(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",false));
                    m.instructions.add(new InsnNode(Opcodes.POP));m.instructions.add(new InsnNode(Opcodes.RETURN));
                    if(module!=null&&module.isNamed())instrumentation.redefineModule(module,Set.of(GuiScenario.class.getModule()),Map.of(),Map.of(),Set.of(),Map.of());
                }
                changed=true;
            }
            if(!changed)throw new IllegalStateException("No chooser wrappers found in "+name);
            ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_MAXS);node.accept(writer);
            if(name.endsWith("SDLDialog"))sdlReady=true;else tinyReady=true;
            GuiScenario.log("Fixture picker installed: "+name);return writer.toByteArray();
        }catch(Throwable e){GuiScenario.log("PICKER TRANSFORM FAILED "+e);return null;}
    }
}
