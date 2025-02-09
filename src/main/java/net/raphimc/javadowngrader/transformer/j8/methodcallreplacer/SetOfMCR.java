/*
 * This file is part of JavaDowngrader - https://github.com/RaphiMC/JavaDowngrader
 * Copyright (C) 2023 RK_01/RaphiMC and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.javadowngrader.transformer.j8.methodcallreplacer;

import net.raphimc.javadowngrader.RuntimeDepCollector;
import net.raphimc.javadowngrader.transformer.DowngradeResult;
import net.raphimc.javadowngrader.transformer.MethodCallReplacer;
import net.raphimc.javadowngrader.util.ASMUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

public class SetOfMCR implements MethodCallReplacer {

    @Override
    public InsnList getReplacement(ClassNode classNode, MethodNode methodNode, String originalName, String originalDesc, RuntimeDepCollector depCollector, DowngradeResult result) {
        final InsnList replacement = new InsnList();

        final Type[] args = Type.getArgumentTypes(originalDesc);
        if (args.length != 1 || args[0].getSort() != Type.ARRAY) {
            final int argCount = args.length;
            if (argCount == 0) {
                replacement.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/util/Collections",
                    "emptySet",
                    "()Ljava/util/Set;"
                ));
                return replacement;
            } else if (argCount == 1) {
                replacement.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "java/util/Collections",
                    "singleton",
                    "(Ljava/lang/Object;)Ljava/util/Set;"
                ));
                return replacement;
            }

            final int freeVarIndex = ASMUtil.getFreeVarIndex(methodNode);

            replacement.add(new TypeInsnNode(Opcodes.NEW, "java/util/HashSet"));
            replacement.add(new InsnNode(Opcodes.DUP));
            replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/HashSet", "<init>", "()V"));
            replacement.add(new VarInsnNode(Opcodes.ASTORE, freeVarIndex));
            for (int i = 0; i < argCount; i++) {
                replacement.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
                replacement.add(new InsnNode(Opcodes.SWAP));
                replacement.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, "java/util/Set", "add", "(Ljava/lang/Object;)Z"));
                replacement.add(new InsnNode(Opcodes.POP));
            }
            replacement.add(new VarInsnNode(Opcodes.ALOAD, freeVarIndex));
        } else {
            replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;"));
            replacement.add(new TypeInsnNode(Opcodes.NEW, "java/util/HashSet"));
            replacement.add(new InsnNode(Opcodes.DUP_X1));
            replacement.add(new InsnNode(Opcodes.SWAP));
            replacement.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/HashSet", "<init>", "(Ljava/util/Collection;)V"));
        }
        replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Collections", "unmodifiableSet", "(Ljava/util/Set;)Ljava/util/Set;"));

        return replacement;
    }

}
