package io.github.astelev.around_planet.compat;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

/** Preserve portal collision results when IP bypasses Sable's collision producer. */
final class SablePortalCollisionPatch implements Opcodes {
    static void apply(ClassNode target) {
        var field = target.fields.stream().filter(f -> f.name.equals("sable$collisionInfo")
                && f.desc.equals("Ldev/ryanhcode/sable/sublevel/entity_collision/SubLevelEntityCollision$CollisionInfo;"))
                .findFirst().orElse(null);
        if (field == null) return; // Sable is optional.
        var callback = target.methods.stream().filter(m -> m.name.contains("sable$moveInject")
                && m.desc.equals("(Lnet/minecraft/world/entity/Entity;ZLnet/minecraft/world/phys/Vec3;Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;)V"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Unsupported Sable collision callback"));
        var move = target.methods.stream().filter(m -> m.name.equals("move")
                && m.desc.equals("(Lnet/minecraft/world/entity/MoverType;Lnet/minecraft/world/phys/Vec3;)V"))
                .findFirst().orElseThrow(() -> new IllegalStateException("Entity.move unavailable"));

        // A previous movement's result must not leak into a move where IP short-circuits
        // the collision producer. Sable fills this field again on its ordinary path.
        var reset = new InsnList();
        reset.add(new VarInsnNode(ALOAD, 0));
        reset.add(new InsnNode(ACONST_NULL));
        reset.add(new FieldInsnNode(PUTFIELD, target.name, field.name, field.desc));
        move.instructions.insert(reset);
        move.maxStack = Math.max(move.maxStack, 2);

        // A regular @Inject cannot name this MixinExtras-renamed handler reliably.
        // When Sable did not calculate collisions, keep the flags already calculated
        // by vanilla/IP and forward the original ground-state operation unchanged.
        var guard = new InsnList();
        var normal = new LabelNode();
        guard.add(new VarInsnNode(ALOAD, 0));
        guard.add(new FieldInsnNode(GETFIELD, target.name, field.name, field.desc));
        guard.add(new JumpInsnNode(IFNONNULL, normal));
        guard.add(new VarInsnNode(ALOAD, 4));
        guard.add(new InsnNode(ICONST_3));
        guard.add(new TypeInsnNode(ANEWARRAY, "java/lang/Object"));
        guard.add(new InsnNode(DUP)); guard.add(new InsnNode(ICONST_0));
        guard.add(new VarInsnNode(ALOAD, 1)); guard.add(new InsnNode(AASTORE));
        guard.add(new InsnNode(DUP)); guard.add(new InsnNode(ICONST_1));
        guard.add(new VarInsnNode(ILOAD, 2));
        guard.add(new MethodInsnNode(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false));
        guard.add(new InsnNode(AASTORE));
        guard.add(new InsnNode(DUP)); guard.add(new InsnNode(ICONST_2));
        guard.add(new VarInsnNode(ALOAD, 3)); guard.add(new InsnNode(AASTORE));
        guard.add(new MethodInsnNode(INVOKEINTERFACE, "com/llamalad7/mixinextras/injector/wrapoperation/Operation",
                "call", "([Ljava/lang/Object;)Ljava/lang/Object;", true));
        guard.add(new InsnNode(POP));
        guard.add(new InsnNode(RETURN));
        guard.add(normal);
        guard.add(new FrameNode(F_SAME, 0, null, 0, null));
        callback.instructions.insert(guard);
        callback.maxStack = Math.max(callback.maxStack, 8);
        System.getLogger("around_planet").log(System.Logger.Level.INFO,
                "Installed per-move Sable/portal collision state compatibility");
    }
    private SablePortalCollisionPatch() {}
}
